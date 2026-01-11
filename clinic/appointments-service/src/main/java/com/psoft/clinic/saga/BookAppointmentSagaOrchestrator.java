package com.psoft.clinic.saga;

import com.psoft.clinic.client.ClientsServiceClient;
import com.psoft.clinic.client.DoctorsServiceClient;
import com.psoft.clinic.client.RemoteCallResult;
import com.psoft.clinic.dto.PatientClientResponse;
import com.psoft.clinic.dto.PhysicianClientResponse;
import com.psoft.clinic.events.AppointmentCreatedEvent;
import com.psoft.clinic.eventsourcing.AppointmentEvent;
import com.psoft.clinic.eventsourcing.AppointmentEventStore;
import com.psoft.clinic.eventsourcing.AppointmentIdGenerator;
import com.psoft.clinic.eventsourcing.AppointmentProjector;
import com.psoft.clinic.messaging.AppointmentEventPublisher;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookAppointmentSagaOrchestrator {

    private final AppointmentRepository appointmentRepository; // read model (projeção)
    private final ClientsServiceClient clientsServiceClient;
    private final DoctorsServiceClient doctorsServiceClient;
    private final AppointmentEventPublisher eventPublisher;
    private final SagaInstanceRepository sagaRepo;

    // ES core
    private final AppointmentIdGenerator idGenerator;
    private final AppointmentEventStore eventStore;
    private final AppointmentProjector projector;

    public SagaResult start(BookAppointmentCommand cmd) {
        String correlationId = UUID.randomUUID().toString();
        System.out.println("🔥🔥🔥 SAGA STARTED correlationId=" + correlationId);

        SagaInstance saga = sagaRepo.save(SagaInstance.builder()
                .correlationId(correlationId)
                .sagaType("BOOK_APPOINTMENT")
                .status("STARTED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        Long appointmentId = null;

        try {
            // 1) validar paciente
            saga.setCurrentStep(BookAppointmentStep.VALIDATE_PATIENT.name());
            saga.setUpdatedAt(Instant.now());
            sagaRepo.save(saga);

            var patientRes = clientsServiceClient.getPatientById(cmd.getPatientId());

            if (patientRes.getStatus() == RemoteCallResult.Status.NOT_FOUND) {
                return fail(saga, "Paciente não encontrado no clients-service.");
            }

            if (patientRes.getStatus() != RemoteCallResult.Status.OK) {
                return fail(saga, "clients-service indisponível: " + patientRes.getError());
            }

            PatientClientResponse patient = patientRes.getBody();


            // 2) validar médico
            saga.setCurrentStep(BookAppointmentStep.VALIDATE_PHYSICIAN.name());
            saga.setUpdatedAt(Instant.now());
            sagaRepo.save(saga);

            var physicianRes = doctorsServiceClient.getPhysicianById(cmd.getPhysicianId());

            if (physicianRes.getStatus() == RemoteCallResult.Status.NOT_FOUND) {
                return fail(saga, "Médico não encontrado no doctors-service.");
            }

            if (physicianRes.getStatus() != RemoteCallResult.Status.OK) {
                return fail(saga, "doctors-service indisponível: " + physicianRes.getError());
            }

            PhysicianClientResponse physician = physicianRes.getBody();


            // 3) conflitos (na projeção)
            saga.setCurrentStep(BookAppointmentStep.CHECK_CONFLICTS.name());
            saga.setUpdatedAt(Instant.now());
            sagaRepo.save(saga);

            if (appointmentRepository.existsByPhysicianIdAndDateTime(cmd.getPhysicianId(), cmd.getDateTime())) {
                return fail(saga, "O médico já tem consulta nesse horário.");
            }
            if (appointmentRepository.existsByPatientIdAndDateTime(cmd.getPatientId(), cmd.getDateTime())) {
                return fail(saga, "O paciente já tem consulta nesse horário.");
            }

            // 4) criar appointment via EVENT SOURCING (sem save direto)
            saga.setCurrentStep(BookAppointmentStep.CREATE_APPOINTMENT.name());
            saga.setUpdatedAt(Instant.now());
            sagaRepo.save(saga);

            appointmentId = idGenerator.nextId();

            AppointmentStatus initialStatus =
                    "ADMIN".equalsIgnoreCase(cmd.getRequestedByRole())
                            ? AppointmentStatus.CONFIRMED
                            : AppointmentStatus.PENDING;

            // grava evento CREATED
            AppointmentEvent created = eventStore.append(
                    appointmentId,
                    "APPOINTMENT_CREATED",
                    correlationId,
                    Map.of(
                            "patientId", cmd.getPatientId(),
                            "physicianId", cmd.getPhysicianId(),
                            "dateTime", cmd.getDateTime().toString(),
                            "reason", cmd.getReason(),
                            "status", initialStatus.name(),
                            "requestedByRole", cmd.getRequestedByRole()
                    )
            );

            // projeção sync
            projector.project(created);

            // guardar appointmentId na saga
            saga.setAppointmentId(appointmentId);
            saga.setUpdatedAt(Instant.now());
            sagaRepo.save(saga);

            // 5) publicar evento Rabbit
            saga.setCurrentStep(BookAppointmentStep.PUBLISH_EVENT.name());
            saga.setUpdatedAt(Instant.now());
            sagaRepo.save(saga);

            AppointmentCreatedEvent integrationEvent = new AppointmentCreatedEvent(
                    appointmentId,
                    cmd.getPatientId(),
                    cmd.getPhysicianId(),
                    cmd.getDateTime(),
                    cmd.getReason()
            );

            eventPublisher.publishCreatedEvent(integrationEvent);

            // opcional: guardar evento de integração publicada (não altera projeção)
            eventStore.append(
                    appointmentId,
                    "APPOINTMENT_CREATED_EVENT_PUBLISHED",
                    correlationId,
                    Map.of("appointmentId", appointmentId)
            );

            // 6) sucesso
            saga.setStatus("BOOKED");
            saga.setError(null);
            saga.setUpdatedAt(Instant.now());
            sagaRepo.save(saga);

            eventStore.append(
                    appointmentId,
                    "BOOKING_COMPLETED",
                    correlationId,
                    Map.of("status", "BOOKED")
            );

            return SagaResult.ok(correlationId, appointmentId);

        } catch (Exception e) {

            // aqui, como o write foi por eventos, a “compensação” é também por eventos
            // (não apagar linhas da tabela appointments como fonte de verdade)
            try {
                if (appointmentId != null) {
                    eventStore.append(
                            appointmentId,
                            "BOOKING_FAILED",
                            correlationId,
                            Map.of("error", e.getMessage(), "step", saga.getCurrentStep())
                    );

                    // compensação: CANCELLED (e projetar)
                    AppointmentEvent cancelled = eventStore.append(
                            appointmentId,
                            "APPOINTMENT_CANCELLED",
                            correlationId,
                            Map.of("reason", "COMPENSATION")
                    );
                    projector.project(cancelled);

                    saga.setStatus("FAILED_COMPENSATED");
                } else {
                    saga.setStatus("FAILED");
                }
            } catch (Exception ignored) {
                saga.setStatus("FAILED_COMPENSATION_ERROR");
            }

            saga.setError("Erro inesperado: " + e.getMessage());
            saga.setUpdatedAt(Instant.now());
            sagaRepo.save(saga);

            return SagaResult.fail(correlationId, saga.getError());
        }
    }

    private SagaResult fail(SagaInstance saga, String error) {
        saga.setStatus("FAILED");
        saga.setError(error);
        saga.setUpdatedAt(Instant.now());
        sagaRepo.save(saga);
        return SagaResult.fail(saga.getCorrelationId(), error);
    }
}
