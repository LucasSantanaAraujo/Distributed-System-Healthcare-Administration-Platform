package com.psoft.clinic.controller;

import com.psoft.clinic.client.ClientsServiceClient; // 👈 Novo import
import com.psoft.clinic.dto.*;
import com.psoft.clinic.model.*;
import com.psoft.clinic.repository.*;
import com.psoft.clinic.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.psoft.clinic.util.ScheduleUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final PhysicianRepository physicianRepository;
    private final ClientsServiceClient clientsServiceClient; // 👈 Novo cliente HTTP

    @PostMapping("/admin")
    public ResponseEntity<?> adminScheduleAppointment(@RequestBody AdminAppointmentRequest request) {

        // 📡 1. Contactar o clients-service
        var patientResponse = clientsServiceClient.getPatientById(request.getPatientId());
        if (patientResponse == null) {
            return ResponseEntity.status(404).body("Paciente não encontrado no clients-service.");
        }
        System.out.println("📡 Paciente recebido do clients-service: " + patientResponse.getName());


        // 2. Buscar o médico localmente
        var physicianOpt = physicianRepository.findById(request.getPhysicianId());
        if (physicianOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Médico não encontrado.");
        }

        var dateTime = DateTimeUtils.truncateToMinute(request.getDateTime());
        var physician = physicianOpt.get();

        // 3. Verificar conflitos
        boolean physicianBusy = appointmentRepository.existsByPhysicianAndDateTime(physician, dateTime);
        if (physicianBusy) {
            return ResponseEntity.badRequest().body("O médico já tem uma consulta marcada neste horário.");
        }

        // ❗ Paciente vem de outro serviço → só guardamos o ID
        Appointment appointment = Appointment.builder()
                .dateTime(dateTime)
                .reason(request.getReason())
                .status(AppointmentStatus.CONFIRMED)
                .physician(physician)
                .build();

        appointmentRepository.save(appointment);

        AppointmentResponse response = AppointmentResponse.builder()
                .id(appointment.getId())
                .dateTime(appointment.getDateTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .patientName(patientResponse.getName()) // 👈 agora mostra o nome real
                .physicianName(physician.getName())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/upcoming")
    public ResponseEntity<?> listUpcomingAppointments() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> upcoming = appointmentRepository.findByDateTimeAfterOrderByDateTimeAsc(now);

        List<AppointmentResponse> response = upcoming.stream().map(appointment ->
                AppointmentResponse.builder()
                        .id(appointment.getId())
                        .dateTime(appointment.getDateTime())
                        .reason(appointment.getReason())
                        .status(appointment.getStatus())
                        .patientName("Paciente remoto") // virá do clients-service futuramente
                        .physicianName(appointment.getPhysician().getName())
                        .build()
        ).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/physicians/{id}/slots")
    public ResponseEntity<?> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Physician physician = physicianRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Physician not found"));

        List<LocalDateTime> slots = ScheduleUtils.generateSlotsFromSchedule(physician.getSchedule(), date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Appointment> appointments = appointmentRepository
                .findByPhysicianAndDateTimeBetween(physician, startOfDay, endOfDay);

        List<LocalDateTime> freeSlots = slots.stream()
                .filter(slot -> appointments.stream()
                        .noneMatch(app -> isSameSlot(slot, app.getDateTime())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(freeSlots);
    }

    private boolean isSameSlot(LocalDateTime slot1, LocalDateTime slot2) {
        return slot1.truncatedTo(ChronoUnit.MINUTES).equals(slot2.truncatedTo(ChronoUnit.MINUTES));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAppointmentById(@PathVariable Long id) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);
        if (appointmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Appointment appointment = appointmentOpt.get();

        AppointmentResponse response = AppointmentResponse.builder()
                .id(appointment.getId())
                .dateTime(appointment.getDateTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .patientName("Paciente remoto") // virá do clients-service futuramente
                .physicianName(appointment.getPhysician().getName())
                .build();

        return ResponseEntity.ok(response);
    }



    @GetMapping("/admin/statistics/age-groups")
    public ResponseEntity<?> getAppointmentStatisticsByAgeGroup() {
        List<Appointment> appointments = appointmentRepository.findAll();

        Map<String, List<Appointment>> grouped = new HashMap<>();

        for (Appointment appointment : appointments) {
            // sem dados locais de paciente → omitimos
            continue;
        }

        return ResponseEntity.ok("Função estatística desativada (pacientes remotos)");
    }
}
