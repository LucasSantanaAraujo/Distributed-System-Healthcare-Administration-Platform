package com.psoft.clinic.saga;

import com.psoft.clinic.client.ClientsServiceClient;
import com.psoft.clinic.client.DoctorsServiceClient;
import com.psoft.clinic.dto.PatientClientResponse;
import com.psoft.clinic.dto.PhysicianClientResponse;
import com.psoft.clinic.events.AppointmentCreatedEvent;
import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookAppointmentSagaOrchestrator {

    private final AppointmentRepository appointmentRepository;
    private final ClientsServiceClient clientsServiceClient;
    private final DoctorsServiceClient doctorsServiceClient;
    private final com.psoft.clinic.messaging.AppointmentEventPublisher eventPublisher;
    private final SagaInstanceRepository sagaRepo;

    public SagaResult start(BookAppointmentCommand cmd) {
        String correlationId = UUID.randomUUID().toString();
        System.out.println("🔥🔥🔥 SAGA STARTED correlationId=" + correlationId);

        // criar saga instance
        sagaRepo.save(SagaInstance.builder()
                .correlationId(correlationId)
                .sagaType("BOOK_APPOINTMENT")
                .status("STARTED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        try {
            // 1) validar paciente
            PatientClientResponse patient = clientsServiceClient.getPatientById(cmd.getPatientId());
            if (patient == null) {
                return fail(correlationId, "Paciente não encontrado no clients-service.");
            }

            // 2) validar médico
            PhysicianClientResponse physician = doctorsServiceClient.getPhysicianById(cmd.getPhysicianId());
            if (physician == null) {
                return fail(correlationId, "Médico não encontrado no doctors-service.");
            }

            // 3) conflitos (mesma lógica que tinhas no controller)
            if (appointmentRepository.existsByPhysicianIdAndDateTime(cmd.getPhysicianId(), cmd.getDateTime())) {
                return fail(correlationId, "O médico já tem consulta nesse horário.");
            }
            if (appointmentRepository.existsByPatientIdAndDateTime(cmd.getPatientId(), cmd.getDateTime())) {
                return fail(correlationId, "O paciente já tem consulta nesse horário.");
            }

            // 4) persistir appointment
            AppointmentStatus initialStatus =
                    "ADMIN".equalsIgnoreCase(cmd.getRequestedByRole())
                            ? AppointmentStatus.CONFIRMED
                            : AppointmentStatus.PENDING;

            Appointment appt = Appointment.builder()
                    .patientId(cmd.getPatientId())
                    .physicianId(cmd.getPhysicianId())
                    .dateTime(cmd.getDateTime())
                    .reason(cmd.getReason())
                    .status(initialStatus)
                    // (se quiseres manter birthDate no admin, depois tratamos com base no patient.getBirthDate())
                    .build();

            appointmentRepository.save(appt);

            // 5) publicar evento (para notes-service criar record automático)
            eventPublisher.publishCreatedEvent(new AppointmentCreatedEvent(
                    appt.getId(),
                    appt.getPatientId(),
                    appt.getPhysicianId(),
                    appt.getDateTime(),
                    appt.getReason()
            ));
            // Nota: na fase 2 vamos adicionar header correlationId no RabbitMQ

            // 6) marcar saga como BOOKED
            SagaInstance s = sagaRepo.findById(correlationId).orElseThrow();
            s.setStatus("BOOKED");
            s.setAppointmentId(appt.getId());
            s.setUpdatedAt(Instant.now());
            sagaRepo.save(s);

            return SagaResult.ok(correlationId, appt.getId());

        } catch (Exception e) {
            return fail(correlationId, "Erro inesperado: " + e.getMessage());
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
}
