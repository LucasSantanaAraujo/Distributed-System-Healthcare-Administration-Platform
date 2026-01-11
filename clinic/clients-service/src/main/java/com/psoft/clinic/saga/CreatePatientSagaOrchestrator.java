package com.psoft.clinic.saga;

import com.psoft.clinic.config.RabbitMQConfig;
import com.psoft.clinic.events.PatientCreatedEvent;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.repository.PatientRepository;
import com.psoft.clinic.service.SequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePatientSagaOrchestrator {

    private final PatientRepository patientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SagaInstanceRepository sagaRepo;
    private final RabbitTemplate rabbitTemplate;

    public SagaResult start(CreatePatientCommand cmd) {
        String correlationId = UUID.randomUUID().toString();

        // 1) Criar registo da saga
        sagaRepo.save(SagaInstance.builder()
                .correlationId(correlationId)
                .sagaType("CREATE_PATIENT")
                .status("STARTED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        Patient patient = null;

        try {
            // 2) Validação: já existe paciente com este userId?
            if (patientRepository.findByUserId(cmd.getUserId()).isPresent()) {
                return fail(correlationId, "Paciente já existe com este User ID.", null);
            }

            // 3) Criar paciente localmente
            patient = new Patient();
            patient.setId(sequenceGeneratorService.generateSequence("patients_sequence"));
            patient.setUserId(cmd.getUserId());
            patient.setName(cmd.getName());
            patient.setBirthDate(cmd.getBirthDate());
            patient.setContact(cmd.getPhone());
            patient.setInsuranceInfo(cmd.getInsuranceInfo());
            patient.setConsent(cmd.getConsent());

            patientRepository.save(patient);

            // 4) Publicar evento (se falhar -> lança exceção -> entra na compensação)
            publishPatientCreated(patient);

            // 5) Marcar saga como COMPLETED
            SagaInstance s = sagaRepo.findById(correlationId).orElseThrow();
            s.setStatus("COMPLETED");
            s.setEntityId(patient.getId());
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);

            return SagaResult.ok(correlationId, patient.getId());

        } catch (Exception e) {
            // Se falhar depois de criar paciente, vamos compensar (apagar)
            return fail(correlationId, "Erro inesperado: " + e.getMessage(), patient);
        }
    }

    private SagaResult fail(String correlationId, String error, Patient patientToDelete) {

        // Compensação: apagar paciente se já foi criado
        if (patientToDelete != null && patientToDelete.getId() != null) {
            try {
                patientRepository.delete(patientToDelete);
                System.out.println("⚠️ Compensação executada: Paciente " + patientToDelete.getId() + " removido.");
            } catch (Exception e) {
                error += " [Falha na Compensação: " + e.getMessage() + "]";
            }
        }

        // Guardar falha na saga
        SagaInstance s = sagaRepo.findById(correlationId).orElse(null);
        if (s != null) {
            s.setStatus("FAILED");
            s.setError(error);
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);
        }

        return SagaResult.fail(correlationId, error);
    }

    /**
     * Agora este método NÃO engole exceções.
     * Se o RabbitMQ falhar, lança RuntimeException para a saga falhar.
     */
    private void publishPatientCreated(Patient p) {
        PatientCreatedEvent event = new PatientCreatedEvent(
                p.getId(),
                p.getUserId(),
                p.getName(),
                p.getContact(),
                p.getBirthDate()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CLIENTS_EXCHANGE,
                    RabbitMQConfig.PATIENT_CREATED_ROUTING_KEY,
                    event
            );
            System.out.println("📨 Evento PatientCreated enviado para o RabbitMQ!");
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao publicar evento RabbitMQ: " + e.getMessage());
            throw new RuntimeException("Falha ao publicar evento PatientCreated no RabbitMQ.", e);
        }
    }
}
