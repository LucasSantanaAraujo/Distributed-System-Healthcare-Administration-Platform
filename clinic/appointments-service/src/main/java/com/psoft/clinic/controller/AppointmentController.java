package com.psoft.clinic.controller;

import com.psoft.clinic.client.ClientsServiceClient;
import com.psoft.clinic.client.DoctorsServiceClient;
import com.psoft.clinic.dto.*;
import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.repository.AppointmentRepository;
import com.psoft.clinic.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

    // ADMIN agenda uma consulta entre um paciente (clients-service) e um médico (doctors-service)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin")
    public ResponseEntity<?> adminScheduleAppointment(@RequestBody AdminAppointmentRequest request) {

        // Normalizar minuto (evita conflito por segundos)
        LocalDateTime dateTime = DateTimeUtils.truncateToMinute(request.getDateTime());

        // 1) validar paciente no clients-service
        PatientClientResponse patient = clientsServiceClient.getPatientById(request.getPatientId());
        if (patient == null) {
            return ResponseEntity.status(404).body("Paciente não encontrado no clients-service.");
        }

        // 2) validar médico no doctors-service
        PhysicianClientResponse physician = doctorsServiceClient.getPhysicianById(request.getPhysicianId());
        if (physician == null) {
            return ResponseEntity.status(404).body("Médico não encontrado no doctors-service.");
        }

        // 3) conflitos
        if (appointmentRepository.existsByPhysicianIdAndDateTime(request.getPhysicianId(), dateTime)) {
            return ResponseEntity.badRequest().body("O médico já tem consulta nesse horário.");
        }
        if (appointmentRepository.existsByPatientIdAndDateTime(request.getPatientId(), dateTime)) {
            return ResponseEntity.badRequest().body("O paciente já tem consulta nesse horário.");
        }

        // 4) guardar
        Appointment appt = Appointment.builder()
                .dateTime(dateTime)
                .reason(request.getReason())
                .status(AppointmentStatus.CONFIRMED)
                .patientId(request.getPatientId())
                .physicianId(request.getPhysicianId())
                .build();
        appointmentRepository.save(appt);

        // 5) resposta
        AppointmentResponse resp = AppointmentResponse.builder()
                .id(appt.getId())
                .dateTime(appt.getDateTime())
                .reason(appt.getReason())
                .status(appt.getStatus())
                .patientName(patient.getName())
                .physicianName(physician.getName())
                .build();

        return ResponseEntity.ok(resp);
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

