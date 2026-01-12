package com.psoft.clinic.saga;

import com.psoft.clinic.events.PatientCreatedEvent;
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

    private final PatientRepository patientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SagaInstanceRepository sagaRepo;
    private final RabbitTemplate rabbitTemplate;

    public SagaResult start(CreatePatientCommand cmd) {
        String correlationId = UUID.randomUUID().toString();

        sagaRepo.save(SagaInstance.builder()
                .correlationId(correlationId)
                .sagaType("CREATE_PATIENT")
                .status("STARTED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        Patient patient = null;

        try {
            // 1) Validação
            if (patientRepository.findByUserId(cmd.getUserId()).isPresent()) {
                return fail(correlationId, "Paciente já existe com este User ID.", null);
            }

// 2) Criar paciente
            patient = new Patient();
            patient.setId(sequenceGeneratorService.generateSequence("patients_sequence"));
            patient.setUserId(cmd.getUserId());
            patient.setName(cmd.getName());
            patient.setBirthDate(cmd.getBirthDate());
            patient.setContact(cmd.getPhone());
            patient.setInsuranceInfo(cmd.getInsuranceInfo());
            patient.setConsent(cmd.getConsent());

            patientRepository.save(patient);

            // 3) Publicar evento (secundário)
            boolean published = publishPatientCreated(patient);

            // 4) Completar saga
            SagaInstance s = sagaRepo.findByCorrelationId(correlationId).orElseThrow();
            s.setStatus("COMPLETED");
            s.setPatientId(patient.getId());         // <-- cria este campo
            s.setEventPublished(published);          // <-- cria este campo
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);

            return SagaResult.ok(correlationId, patient.getId());

        } catch (Exception e) {
            return fail(correlationId, "Erro inesperado: " + e.getMessage(), patient);
        }
    }

    private SagaResult fail(String correlationId, String error, Patient patientToDelete) {

        if (patientToDelete != null && patientToDelete.getId() != null) {
            try {
                patientRepository.deleteById(patientToDelete.getId());
                System.out.println("⚠️ Compensação executada: Paciente " + patientToDelete.getId() + " removido.");
            } catch (Exception e) {
                error += " [Falha na Compensação: " + e.getMessage() + "]";
            }
        }

        SagaInstance s = sagaRepo.findByCorrelationId(correlationId).orElse(null);
        if (s != null) {
            s.setStatus("FAILED");
            s.setError(error);
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);
        }

        return SagaResult.fail(correlationId, error);
    }

    private boolean publishPatientCreated(Patient p) {
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
            return true;

        } catch (Exception e) {
            System.err.println("⚠️ Erro ao publicar evento RabbitMQ: " + e.getMessage());
            return false;
        }
    }
}
