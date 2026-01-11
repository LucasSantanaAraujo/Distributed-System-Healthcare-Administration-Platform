package com.psoft.clinic.saga;

import com.psoft.clinic.client.IdentityServiceClient;
import com.psoft.clinic.events.PatientCreatedEvent;
import com.psoft.clinic.eventsourcing.PatientEvent;
import com.psoft.clinic.eventsourcing.PatientEventStore;
import com.psoft.clinic.eventsourcing.PatientProjector;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.repository.PatientRepository;
import com.psoft.clinic.service.SequenceGeneratorService;
import com.psoft.clinic.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePatientSagaOrchestrator {

    private final PatientRepository patientRepository; // Apenas para leitura (validação)
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SagaInstanceRepository sagaRepo;
    private final RabbitTemplate rabbitTemplate;

    // Novos componentes
    private final PatientEventStore eventStore;
    private final PatientProjector projector;
    private final IdentityServiceClient identityService; // Circuit Breaker aqui

    public SagaResult start(CreatePatientCommand cmd) {
        String correlationId = UUID.randomUUID().toString();

        sagaRepo.save(SagaInstance.builder()
                .correlationId(correlationId)
                .sagaType("CREATE_PATIENT")
                .status("STARTED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        try {
            // 1. Validação com Circuit Breaker (Chamada Externa)
            if (!identityService.isValidUser(cmd.getUserId())) {
                return fail(correlationId, "Utilizador inválido ou Serviço de Identidade incontactável.");
            }

            // 2. Validação Local (Read Model)
            if (patientRepository.findByUserId(cmd.getUserId()).isPresent()) {
                return fail(correlationId, "Paciente já existe com este User ID.");
            }

            // 3. Gerar ID
            Long newId = sequenceGeneratorService.generateSequence("patients_sequence");

            // 4. EVENT SOURCING: Gravar Evento em vez de Estado
            // O Event Store é a fonte da verdade. Se gravar aqui, aconteceu.
            PatientEvent evt = eventStore.append(newId, "PATIENT_CREATED", correlationId, cmd);

            // 5. PROJEÇÃO SÍNCRONA (Para consistência imediata na leitura dentro da própria Saga)
            // Em sistemas puramente assíncronos, isto seria feito por um Listener,
            // mas aqui garantimos que o modelo de leitura está pronto antes de continuar.
            projector.project(evt);

            // 6. Publicar Evento de Integração (RabbitMQ)
            publishIntegrationEvent(newId, cmd);

            // 7. Atualizar Saga
            SagaInstance s = sagaRepo.findById(correlationId).orElseThrow();
            s.setStatus("COMPLETED");
            s.setAppointmentId(newId);
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);

            return SagaResult.ok(correlationId, newId);

        } catch (Exception e) {
            // Compensação em Event Sourcing = Evento de Compensação (ex: PATIENT_DELETED)
            // Mas como falhou *durante* a criação, apenas marcamos a Saga como falhada.
            return fail(correlationId, "Erro crítico: " + e.getMessage());
        }
    }

    private SagaResult fail(String correlationId, String error) {
        SagaInstance s = sagaRepo.findById(correlationId).orElse(null);
        if (s != null) {
            s.setStatus("FAILED");
            s.setError(error);
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);
        }
        return SagaResult.fail(correlationId, error);
    }

    private void publishIntegrationEvent(Long id, CreatePatientCommand cmd) {
        try {
            PatientCreatedEvent event = new PatientCreatedEvent(
                    id, cmd.getUserId(), cmd.getName(), cmd.getPhone(), cmd.getBirthDate()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CLIENTS_EXCHANGE,
                    RabbitMQConfig.PATIENT_CREATED_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            System.err.println("⚠️ Erro RabbitMQ (Saga não falha): " + e.getMessage());
        }
    }
}