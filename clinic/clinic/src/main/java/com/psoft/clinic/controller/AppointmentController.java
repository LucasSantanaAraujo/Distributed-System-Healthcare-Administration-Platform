package com.psoft.clinic.controller;

import com.psoft.clinic.dto.*;
import com.psoft.clinic.model.*;
import com.psoft.clinic.repository.*;
import com.psoft.clinic.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.psoft.clinic.util.ScheduleUtils;

import com.psoft.clinic.dto.PatientAppointmentRequest;
import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.repository.AppointmentRepository;
import com.psoft.clinic.repository.PhysicianRepository;
import com.psoft.clinic.repository.UserRepository;
import java.security.Principal;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Period;
import java.time.Duration;



@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PhysicianRepository physicianRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRecordRepository appointmentRecordRepository;



    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin")
    public ResponseEntity<?> adminScheduleAppointment(@RequestBody AdminAppointmentRequest request) {


        var patientOpt = patientRepository.findById(request.getPatientId());
        if (patientOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Paciente não encontrado.");
        }

        var physicianOpt = physicianRepository.findById(request.getPhysicianId());
        if (physicianOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Médico não encontrado.");
        }
        var dateTime = DateTimeUtils.truncateToMinute(request.getDateTime());
        var patient = patientOpt.get();
        var physician = physicianOpt.get();

        // ❗ Verificar se o médico já tem consulta nesse horário
        boolean physicianBusy = appointmentRepository.existsByPhysicianAndDateTime(physician, dateTime);
        if (physicianBusy) {
            return ResponseEntity.badRequest().body("O médico já tem uma consulta marcada neste horário.");
        }

        // ❗ Verificar se o paciente já tem consulta nesse horário
        boolean patientBusy = appointmentRepository.existsByPatientAndDateTime(patient, dateTime);
        if (patientBusy) {
            return ResponseEntity.badRequest().body("O paciente já tem uma consulta marcada neste horário.");
        }

        Appointment appointment = Appointment.builder()
                .dateTime(dateTime)
                .reason(request.getReason())
                .status(AppointmentStatus.CONFIRMED)
                .patient(patientOpt.get())
                .physician(physicianOpt.get())
                .build();

        appointmentRepository.save(appointment);

        AppointmentResponse response = AppointmentResponse.builder()
                .id(appointment.getId())
                .dateTime(appointment.getDateTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .patientName(appointment.getPatient().getName())
                .physicianName(appointment.getPhysician().getName())
                .build();

        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/history")
    public ResponseEntity<?> getPatientHistory(@AuthenticationPrincipal(expression = "username") String email) {
        User user = userRepository.findByEmail(email).orElseThrow();

        if (user.getPatient() == null) {
            return ResponseEntity.badRequest().body("Este utilizador não está associado a um paciente.");
        }

        var appointments = appointmentRepository.findByPatient(user.getPatient());

        var responses = appointments.stream().map(appointment -> AppointmentResponse.builder()
                .id(appointment.getId())
                .dateTime(appointment.getDateTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .patientName(appointment.getPatient().getName())
                .physicianName(appointment.getPhysician().getName())
                .diagnosis(appointment.getRecord() != null ? appointment.getRecord().getDiagnosis() : null)
                .treatment(appointment.getRecord() != null ? appointment.getRecord().getTreatment() : null)
                .prescription(appointment.getRecord() != null ? appointment.getRecord().getPrescription() : null)
                .build()
        ).toList();

        return ResponseEntity.ok(responses);
    }


    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @PutMapping("/{id}/update-or-cancel")
    public ResponseEntity<?> updateOrCancelAppointment(@PathVariable Long id,
                                                       @RequestBody UpdateAppointmentRequest request,
                                                       @AuthenticationPrincipal(expression = "username") String email) {

        User user = userRepository.findByEmail(email).orElseThrow();
        Appointment appointment = appointmentRepository.findById(id).orElse(null);

        if (appointment == null) {
            return ResponseEntity.notFound().build();
        }

        // Verificar se o paciente é dono da consulta (caso não seja admin)
        if (user.getRole() == Role.PATIENT &&
                (user.getPatient() == null || !appointment.getPatient().getId().equals(user.getPatient().getId()))) {
            return ResponseEntity.status(403).body("Não podes modificar consultas de outro paciente.");
        }

        if (request.isCancel()) {
            // Cancelamento
            appointment.setStatus(AppointmentStatus.CANCELLED);
        } else if (request.getDateTime() != null && !appointment.getDateTime().equals(request.getDateTime())) {
            // Remarcar: Se a nova data/hora é diferente da atual
            appointment.setDateTime(request.getDateTime());
            appointment.setStatus(AppointmentStatus.RESCHEDULED);
        }

        appointmentRepository.save(appointment);
        return ResponseEntity.ok("Consulta atualizada com sucesso.");
    }




    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable Long id,
                                                     @RequestParam("status") String status) {

        Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);
        if (appointmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Appointment appointment = appointmentOpt.get();

        try {
            AppointmentStatus newStatus = AppointmentStatus.valueOf(status.toUpperCase());
            if (newStatus == AppointmentStatus.CONFIRMED) {
                return ResponseEntity.badRequest().body("Não é permitido voltar ao estado PENDING.");
            }
            appointment.setStatus(newStatus);
            appointmentRepository.save(appointment);

            return ResponseEntity.ok("Consulta atualizada para " + newStatus);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Status inválido. Use CONFIRMED ou CANCELLED.");
        }



    }

    @PreAuthorize("hasRole('PHYSICIAN')")
    @GetMapping("/physician/history")
    public ResponseEntity<?> getPhysicianHistory(@AuthenticationPrincipal(expression = "username") String email) {
        User user = userRepository.findByEmail(email).orElseThrow();

        if (user.getRole() != Role.PHYSICIAN || user.getPhysician() == null) {
            return ResponseEntity.badRequest().body("Este utilizador não está associado a um médico.");
        }

        var appointments = appointmentRepository.findByPhysician(user.getPhysician());

        var responses = appointments.stream().map(appointment -> AppointmentResponse.builder()
                .id(appointment.getId())
                .dateTime(appointment.getDateTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .patientName(appointment.getPatient().getName())
                .physicianName(appointment.getPhysician().getName())
                .diagnosis(appointment.getRecord() != null ? appointment.getRecord().getDiagnosis() : null)
                .treatment(appointment.getRecord() != null ? appointment.getRecord().getTreatment() : null)
                .prescription(appointment.getRecord() != null ? appointment.getRecord().getPrescription() : null)
                .build()
        ).toList();

        return ResponseEntity.ok(responses);
    }
    @PreAuthorize("hasRole('PHYSICIAN')")
    @PutMapping("/{id}/details")
    public ResponseEntity<?> addAppointmentDetails(@PathVariable Long id,
                                                   @RequestBody AppointmentDetailsRequest request,
                                                   @AuthenticationPrincipal(expression = "username") String email) {

        User user = userRepository.findByEmail(email).orElseThrow();

        if (user.getPhysician() == null) {
            return ResponseEntity.badRequest().body("Este utilizador não está associado a um médico.");
        }

        Appointment appointment = appointmentRepository.findById(id).orElse(null);

        if (appointment == null) {
            return ResponseEntity.notFound().build();
        }

        if (!appointment.getPhysician().getId().equals(user.getPhysician().getId())) {
            return ResponseEntity.status(403).body("Não podes editar consultas de outro médico.");
        }

        // Criar e associar o AppointmentRecord
        AppointmentRecord record = AppointmentRecord.builder()
                .appointment(appointment)
                .diagnosis(request.getDiagnosis())
                .treatment(request.getTreatment())
                .prescription(request.getPrescription())
                .build();

        appointment.setRecord(record);
        appointmentRepository.save(appointment);

        return ResponseEntity.ok("Detalhes da consulta atualizados com sucesso.");
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'PATIENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getAppointmentById(@PathVariable Long id,
                                                @AuthenticationPrincipal(expression = "username") String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);

        if (appointmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Appointment appointment = appointmentOpt.get();

        // Verifica se é ADMIN ou se é o dono da consulta
        boolean isAdmin = user.getRole().equals(Role.ADMIN);
        boolean isOwner = user.getRole().equals(Role.PATIENT) &&
                user.getPatient() != null &&
                appointment.getPatient().getId().equals(user.getPatient().getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(403).body("Não tens permissão para ver esta consulta.");
        }

        AppointmentResponse response = AppointmentResponse.builder()
                .id(appointment.getId())
                .dateTime(appointment.getDateTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .patientName(appointment.getPatient().getName())
                .physicianName(appointment.getPhysician().getName())
                .diagnosis(appointment.getRecord() != null ? appointment.getRecord().getDiagnosis() : null)
                .treatment(appointment.getRecord() != null ? appointment.getRecord().getTreatment() : null)
                .prescription(appointment.getRecord() != null ? appointment.getRecord().getPrescription() : null)
                .build();

        return ResponseEntity.ok(response);
    }



    @PreAuthorize("hasRole('PHYSICIAN')")
    @GetMapping("/physician/patient/{patientId}")
    public ResponseEntity<?> getAppointmentsByPatientForPhysician(@PathVariable Long patientId,
                                                                  @AuthenticationPrincipal(expression = "username") String email) {

        // Buscar médico autenticado
        User user = userRepository.findByEmail(email).orElseThrow();
        if (user.getPhysician() == null) {
            return ResponseEntity.status(403).body("Este utilizador não está associado a um médico.");
        }

        // Buscar consultas do paciente com este médico
        var appointments = appointmentRepository.findByPhysicianAndPatientId(user.getPhysician(), patientId);

        var responses = appointments.stream().map(appointment -> AppointmentResponse.builder()
                .id(appointment.getId())
                .dateTime(appointment.getDateTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .patientName(appointment.getPatient().getName())
                .physicianName(appointment.getPhysician().getName())
                .diagnosis(appointment.getRecord() != null ? appointment.getRecord().getDiagnosis() : null)
                .treatment(appointment.getRecord() != null ? appointment.getRecord().getTreatment() : null)
                .prescription(appointment.getRecord() != null ? appointment.getRecord().getPrescription() : null)
                .build()
        ).toList();

        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PATIENT')")
    @GetMapping("/records/{recordId}")
    public ResponseEntity<?> getAppointmentRecordById(@PathVariable Long recordId,
                                                      @AuthenticationPrincipal(expression = "username") String email) {
        User user = userRepository.findByEmail(email).orElseThrow();

        Optional<AppointmentRecord> recordOpt = appointmentRecordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppointmentRecord record = recordOpt.get();
        Appointment appointment = record.getAppointment();

        boolean isAdmin = user.getRole().equals(Role.ADMIN);
        boolean isOwner = user.getRole().equals(Role.PATIENT) &&
                user.getPatient() != null &&
                appointment.getPatient().getId().equals(user.getPatient().getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(403).body("Não tens permissão para ver este registo.");
        }

        var response = AppointmentResponse.builder()
                .id(appointment.getId())
                .dateTime(appointment.getDateTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .patientName(appointment.getPatient().getName())
                .physicianName(appointment.getPhysician().getName())
                .diagnosis(record.getDiagnosis())
                .treatment(record.getTreatment())
                .prescription(record.getPrescription())
                .build();

        return ResponseEntity.ok(response);
    }



    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @GetMapping("/physicians/{id}/slots")
    public ResponseEntity<?> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // 1. Buscar o médico
        Physician physician = physicianRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Physician not found"));

        // 2. Gerar todos os slots possíveis desse médico nesse dia
        List<LocalDateTime> slots = ScheduleUtils.generateSlotsFromSchedule(physician.getSchedule(), date);
        System.out.println("Slots gerados: " + slots);

        // 3. Definir intervalo do dia
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // 4. Buscar as consultas já marcadas para esse médico nesse dia
        List<Appointment> appointments = appointmentRepository
                .findByPhysicianAndDateTimeBetween(physician, startOfDay, endOfDay);

        // 5. Filtrar os slots livres (nenhum appointment marcado para esse horário)
        List<LocalDateTime> freeSlots = slots.stream()
                .filter(slot -> appointments.stream()
                        .noneMatch(app -> isSameSlot(slot, app.getDateTime())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(freeSlots);
    }

    // Função auxiliar para comparar slots pelo minuto
    private boolean isSameSlot(LocalDateTime slot1, LocalDateTime slot2) {
        return slot1.truncatedTo(ChronoUnit.MINUTES).equals(slot2.truncatedTo(ChronoUnit.MINUTES));



    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/upcoming")
    public ResponseEntity<?> listUpcomingAppointments() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> upcoming = appointmentRepository.findByDateTimeAfterOrderByDateTimeAsc(now);

        // Mapear cada Appointment para o DTO (AppointmentResponse)
        List<AppointmentResponse> response = upcoming.stream().map(appointment ->
                AppointmentResponse.builder()
                        .id(appointment.getId())
                        .dateTime(appointment.getDateTime())
                        .reason(appointment.getReason())
                        .status(appointment.getStatus())
                        .patientName(appointment.getPatient().getName())
                        .physicianName(appointment.getPhysician().getName())
                        .build()
        ).toList();

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("patient")
    public ResponseEntity<?> scheduleAppointmentAsPatient(@RequestBody PatientAppointmentRequest request,
                                                          Principal principal) {
        var userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        var user = userOpt.get();
        var patient = user.getPatient();
        if (patient == null) {
            return ResponseEntity.status(403).body("Only patients can schedule appointments");
        }

        var physicianOpt = physicianRepository.findById(request.getPhysicianId());
        if (physicianOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Physician not found");
        }

        var physician = physicianOpt.get();

        var conflicts = appointmentRepository.findByPhysicianIdAndDateTime(
                physician.getId(), request.getDateTime());

        if (!conflicts.isEmpty()) {
            return ResponseEntity.status(409).body("Physician is not available at that time");
        }

        var appointment = Appointment.builder()
                .physician(physician)
                .patient(patient)
                .dateTime(request.getDateTime())
                .reason(request.getConsultationType())
                .status(AppointmentStatus.PENDING)
                .build();

        appointmentRepository.save(appointment);

        return ResponseEntity.ok("Appointment scheduled successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/statistics/age-groups")
    public ResponseEntity<?> getAppointmentStatisticsByAgeGroup() {
        List<Appointment> appointments = appointmentRepository.findAll();

        Map<String, List<Appointment>> grouped = new HashMap<>();

        for (Appointment appointment : appointments) {
            if (appointment.getPatient() == null || appointment.getPatient().getBirthDate() == null) continue;

            int age = Period.between(appointment.getPatient().getBirthDate(), appointment.getDateTime().toLocalDate()).getYears();
            String group;
            if (age < 18) group = "Under 18";
            else if (age <= 30) group = "18–30";
            else if (age <= 50) group = "31–50";
            else group = "Above 50";

            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(appointment);
        }

        List<AgeGroupStatisticsResponse> results = grouped.entrySet().stream().map(entry -> {
            List<Appointment> groupAppointments = entry.getValue();
            int total = groupAppointments.size();

            // Cálculo da duração média
            double avgDuration = groupAppointments.stream()
                    .filter(app -> app.getRecord() != null)
                    .mapToLong(app -> java.time.Duration.between(app.getDateTime(), app.getRecord().getCreatedAt()).toMinutes())
                    .average()
                    .orElse(0);

            return new AgeGroupStatisticsResponse(entry.getKey(), total, avgDuration);
        }).toList();

        return ResponseEntity.ok(results);
    }








}
