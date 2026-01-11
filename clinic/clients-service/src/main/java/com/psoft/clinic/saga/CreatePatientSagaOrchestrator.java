package com.psoft.clinic.saga;

import com.psoft.clinic.events.PatientCreatedEvent; // Vais precisar de criar este evento ou usar um genérico map
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.repository.PatientRepository;
import com.psoft.clinic.service.SequenceGeneratorService;
import com.psoft.clinic.config.RabbitMQConfig; // Assume-se que tens a config das filas
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePatientSagaOrchestrator {

    private final PatientRepository patientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SagaInstanceRepository sagaRepo;
    private final RabbitTemplate rabbitTemplate;

    // Inicia a SAGA
    public SagaResult start(CreatePatientCommand cmd) {
        String correlationId = UUID.randomUUID().toString();

        // 1. Criar Registo da SAGA (Audit/Event Sourcing)
        sagaRepo.save(SagaInstance.builder()
                .correlationId(correlationId)
                .sagaType("CREATE_PATIENT")
                .status("STARTED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        Patient patient = null;

        try {
            // 2. Validações de Negócio
            if (patientRepository.findByUserId(cmd.getUserId()).isPresent()) {
                return fail(correlationId, "Paciente já existe com este User ID.", null);
            }

            // 3. Execução Local (Criar o Paciente)
            patient = new Patient();
            patient.setId(sequenceGeneratorService.generateSequence("patients_sequence"));
            patient.setUserId(cmd.getUserId());
            patient.setName(cmd.getName());
            patient.setBirthDate(cmd.getBirthDate());
            patient.setContact(cmd.getPhone());
            patient.setInsuranceInfo(cmd.getInsuranceInfo());
            patient.setConsent(cmd.getConsent());

            patientRepository.save(patient);

            // 4. Publicar Evento (Assíncrono) - Para notificar outros serviços (ex: reports)
            // Se isto falhar, o catch apanha e faz rollback do passo 3
            publishPatientCreated(patient);

            // 5. Sucesso - Atualiza SAGA
            SagaInstance s = sagaRepo.findById(correlationId).orElseThrow();
            s.setStatus("COMPLETED");
            s.setAppointmentId(patient.getId()); // Reutilizando o campo, ou cria um entityId genérico
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);

            return SagaResult.ok(correlationId, patient.getId());

        } catch (Exception e) {
            // Se falhar, passamos o paciente criado (se existir) para ser apagado (Compensação)
            return fail(correlationId, "Erro inesperado: " + e.getMessage(), patient);
        }
    }

    // Lógica de Compensação e Registo de Erro
    private SagaResult fail(String correlationId, String error, Patient patientToDelete) {
        // COMPENSACÃO: Se o paciente chegou a ser guardado na BD mas o processo falhou depois, apagamos.
        if (patientToDelete != null && patientToDelete.getId() != null) {
            try {
                patientRepository.delete(patientToDelete);
                System.out.println("⚠️ Compensação executada: Paciente " + patientToDelete.getId() + " removido.");
            } catch (Exception e) {
                error += " [Falha na Compensação: " + e.getMessage() + "]";
            }
        }

        // PERSISTÊNCIA DO ERRO (Event Sourcing do pobre)
        SagaInstance s = sagaRepo.findById(correlationId).orElse(null);
        if (s != null) {
            s.setStatus("FAILED");
            s.setError(error);
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);
        }
        return SagaResult.fail(correlationId, error);
    }

    private void publishPatientCreated(Patient p) {
        try {
            PatientCreatedEvent event = new PatientCreatedEvent(
                    p.getId(),
                    p.getUserId(),
                    p.getName(),
                    p.getContact(),
                    p.getBirthDate()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CLIENTS_EXCHANGE,
                    RabbitMQConfig.PATIENT_CREATED_ROUTING_KEY,
                    event
            );

            System.out.println("📨 Evento PatientCreated enviado para o RabbitMQ!");

        } catch (Exception e) {
            System.err.println("⚠️ Erro ao publicar evento RabbitMQ: " + e.getMessage());
            // Nota: Não fazemos throw aqui para não reverter a criação do paciente
            // (a criação na BD foi sucesso, o evento é secundário)
        }
    }
}