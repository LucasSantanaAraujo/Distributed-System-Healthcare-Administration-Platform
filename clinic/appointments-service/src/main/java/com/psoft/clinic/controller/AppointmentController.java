package com.psoft.clinic.controller;

import com.psoft.clinic.client.ClientsServiceClient;
import com.psoft.clinic.client.DoctorsServiceClient;
import com.psoft.clinic.dto.*;
import com.psoft.clinic.events.AppointmentCreatedEvent;
import com.psoft.clinic.messaging.AppointmentEventPublisher;
import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.repository.AppointmentRepository;
import com.psoft.clinic.saga.BookAppointmentCommand;
import com.psoft.clinic.saga.BookAppointmentSagaOrchestrator;
import com.psoft.clinic.saga.SagaResult;
import com.psoft.clinic.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final ClientsServiceClient clientsServiceClient;
    private final DoctorsServiceClient doctorsServiceClient;
    private final AppointmentEventPublisher eventPublisher;
    private final BookAppointmentSagaOrchestrator saga;


    // ADMIN agenda uma consulta entre um paciente (clients-service) e um médico (doctors-service)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin")
    public ResponseEntity<?> adminScheduleAppointment(@RequestBody AdminAppointmentRequest request) {

        System.out.println("➡️ Controller ADMIN chamou a saga");

        LocalDateTime dateTime = DateTimeUtils.truncateToMinute(request.getDateTime());

        SagaResult result = saga.start(new BookAppointmentCommand(
                request.getPatientId(),
                request.getPhysicianId(),
                dateTime,
                request.getReason(),
                "ADMIN"
        ));

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result.getError());
        }

        Optional<Appointment> apptOpt = appointmentRepository.findById(result.getAppointmentId());
        if (apptOpt.isEmpty()) {
            return ResponseEntity.status(500).body("Consulta criada mas não encontrada.");
        }

        Appointment appt = apptOpt.get();

        String patientName = "Paciente #" + appt.getPatientId();
        String physicianName = "Médico #" + appt.getPhysicianId();

        try {
            PatientClientResponse patient = clientsServiceClient.getPatientById(appt.getPatientId());
            if (patient != null) patientName = patient.getName();
        } catch (Exception ignored) {}

        try {
            PhysicianClientResponse physician = doctorsServiceClient.getPhysicianById(appt.getPhysicianId());
            if (physician != null) physicianName = physician.getName();
        } catch (Exception ignored) {}

        AppointmentResponse resp = AppointmentResponse.builder()
                .id(appt.getId())
                .dateTime(appt.getDateTime())
                .reason(appt.getReason())
                .status(appt.getStatus())
                .patientName(patientName)
                .physicianName(physicianName)
                .build();

        return ResponseEntity.ok()
                .header("X-Correlation-Id", result.getCorrelationId())
                .body(resp);
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<?> getAppointmentInternal(@PathVariable Long id) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);
        if (appointmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Appointment appointment = appointmentOpt.get();

        AppointmentBasicResponse response = AppointmentBasicResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatientId())
                .physicianId(appointment.getPhysicianId())
                .status(appointment.getStatus().toString())
                .reason(appointment.getReason())
                .dateTime(appointment.getDateTime().toString())
                .build();

        return ResponseEntity.ok(response);
    }



    // ADMIN → lista próximas consultas
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/upcoming")
    public ResponseEntity<?> listUpcomingAppointments() {
        List<Appointment> upcoming = appointmentRepository.findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime.now());

        // Enriquecer com nomes remoto (best-effort; se falhar mostramos IDs)
        List<AppointmentResponse> response = upcoming.stream().map(a -> {
            String patientName = "Paciente #" + a.getPatientId();
            String physicianName = "Médico #" + a.getPhysicianId();

            try {
                var p = clientsServiceClient.getPatientById(a.getPatientId());
                if (p != null) patientName = p.getName();
            } catch (Exception ignored) {}

            try {
                var d = doctorsServiceClient.getPhysicianById(a.getPhysicianId());
                if (d != null) physicianName = d.getName();
            } catch (Exception ignored) {}

            return AppointmentResponse.builder()
                    .id(a.getId())
                    .dateTime(a.getDateTime())
                    .reason(a.getReason())
                    .status(a.getStatus())
                    .patientName(patientName)
                    .physicianName(physicianName)
                    .build();
        }).toList();

        return ResponseEntity.ok(response);
    }

    // ADMIN → obter consulta por id
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Appointment> opt = appointmentRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Appointment a = opt.get();

        String patientName = "Paciente #" + a.getPatientId();
        String physicianName = "Médico #" + a.getPhysicianId();

        try {
            var p = clientsServiceClient.getPatientById(a.getPatientId());
            if (p != null) patientName = p.getName();
        } catch (Exception ignored) {}

        try {
            var d = doctorsServiceClient.getPhysicianById(a.getPhysicianId());
            if (d != null) physicianName = d.getName();
        } catch (Exception ignored) {}

        return ResponseEntity.ok(
                AppointmentResponse.builder()
                        .id(a.getId())
                        .dateTime(a.getDateTime())
                        .reason(a.getReason())
                        .status(a.getStatus())
                        .patientName(patientName)
                        .physicianName(physicianName)
                        .build()
        );
    }

    // PATIENT → marca consulta (recebe patientId no body)
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/patient")
    public ResponseEntity<?> scheduleAsPatient(@RequestBody PatientAppointmentRequest request) {

        LocalDateTime dateTime = DateTimeUtils.truncateToMinute(request.getDateTime());

        // validar médico
        PhysicianClientResponse physician = doctorsServiceClient.getPhysicianById(request.getPhysicianId());
        if (physician == null) {
            return ResponseEntity.status(404).body("Médico não encontrado no doctors-service.");
        }

        // validar paciente
        PatientClientResponse patient = clientsServiceClient.getPatientById(request.getPatientId());
        if (patient == null) {
            return ResponseEntity.status(404).body("Paciente não encontrado no clients-service.");
        }

        // conflitos
        if (appointmentRepository.existsByPhysicianIdAndDateTime(request.getPhysicianId(), dateTime)) {
            return ResponseEntity.status(409).body("Médico indisponível nesse horário.");
        }
        if (appointmentRepository.existsByPatientIdAndDateTime(request.getPatientId(), dateTime)) {
            return ResponseEntity.status(409).body("Já tens consulta marcada nesse horário.");
        }

        // criar
        Appointment appt = Appointment.builder()
                .physicianId(request.getPhysicianId())
                .patientId(request.getPatientId())
                .dateTime(dateTime)
                .reason(request.getConsultationType())
                .status(AppointmentStatus.PENDING)
                .build();
        appointmentRepository.save(appt);

        return ResponseEntity.ok("Consulta marcada com sucesso.");
    }

    // ADMIN/PATIENT → alterar estado (CONFIRMED/CANCELLED/RESCHEDULED)
    @PreAuthorize("hasAnyRole('ADMIN','PATIENT')")
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam("status") String status) {
        var opt = appointmentRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            AppointmentStatus newStatus = AppointmentStatus.valueOf(status.toUpperCase());
            if (newStatus == AppointmentStatus.CONFIRMED) {
                // regra original: não voltar a PENDING; aqui mantemos a regra de não "regredir" arbitrariamente
            }
            Appointment a = opt.get();
            a.setStatus(newStatus);
            appointmentRepository.save(a);
            return ResponseEntity.ok("Consulta atualizada para " + newStatus);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Status inválido. Use CONFIRMED, CANCELLED ou RESCHEDULED.");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/statistics/age-groups")
    public ResponseEntity<?> getAppointmentStatisticsByAgeGroup() {
        List<Appointment> appointments = appointmentRepository.findAll();

        if (appointments.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // ⚙️ Agrupamento por faixa etária
        Map<String, List<Appointment>> grouped = new HashMap<>();

        for (Appointment appointment : appointments) {
            // ⚠️ Garante que temos data de nascimento
            if (appointment.getPatientBirthDate() == null) continue;

            int age = Period.between(appointment.getPatientBirthDate(), appointment.getDateTime().toLocalDate()).getYears();

            String group;
            if (age < 18) group = "Under 18";
            else if (age <= 30) group = "18–30";
            else if (age <= 50) group = "31–50";
            else group = "Above 50";

            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(appointment);
        }

        // 📊 Monta resposta com total e duração média
        List<AgeGroupStatisticsResponse> results = grouped.entrySet().stream()
                .map(entry -> {
                    List<Appointment> groupAppointments = entry.getValue();
                    int total = groupAppointments.size();

                    double avgDuration = groupAppointments.stream()
                            .filter(app -> app.getRecordCreatedAt() != null)
                            .mapToLong(app -> java.time.Duration.between(app.getDateTime(), app.getRecordCreatedAt()).toMinutes())
                            .average()
                            .orElse(0);

                    return new AgeGroupStatisticsResponse(entry.getKey(), total, avgDuration);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }
}

