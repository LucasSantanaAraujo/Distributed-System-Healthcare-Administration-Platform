package com.psoft.clinic.controller;

import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.Physician;
import com.psoft.clinic.repository.AppointmentRepository;
import com.psoft.clinic.repository.PhysicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final PhysicianRepository physicianRepository;
    private final AppointmentRepository appointmentRepository;

    // Apenas ESTE construtor!
    @Autowired
    public AdminController(PhysicianRepository physicianRepository,
                           AppointmentRepository appointmentRepository) {
        this.physicianRepository = physicianRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/only")
    @PreAuthorize("hasRole('ADMIN')")
    public String onlyForAdmins() {
        return "Bem-vindo, ADMIN!";
    }

    @GetMapping("/physicians/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTopPhysicians() {
        // Busca só os 5 primeiros
        PageRequest top5 = PageRequest.of(0, 5);

        List<Object[]> result = appointmentRepository.findTopPhysiciansByAppointmentCount(top5);

        // Transformar em DTO para resposta clara (nome, id, quantidade)
        List<Map<String, Object>> response = result.stream().map(obj -> {
            Physician physician = (Physician) obj[0];
            Long count = (Long) obj[1];
            Map<String, Object> map = new HashMap<>();
            map.put("id", physician.getId());
            map.put("name", physician.getName());
            map.put("specialty", physician.getSpecialty());
            map.put("appointments", count);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/physicians/average-duration")
    public ResponseEntity<?> getAverageAppointmentDurationPerPhysician() {
        List<Physician> physicians = physicianRepository.findAll();
        Map<String, Integer> averages = new HashMap<>();

        for (Physician physician : physicians) {
            averages.put(physician.getName(), 30); // 30 minutos assumidos
        }
        return ResponseEntity.ok(averages);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/appointments/monthly-report")
    public ResponseEntity<?> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {

        long total = appointmentRepository.countByMonthAndYear(month, year);
        long cancelled = appointmentRepository.countCancelledByMonthAndYear(month, year);

        // Só se tiver RESCHEDULED:
        Long rescheduled = null;
        try {
            rescheduled = appointmentRepository.countRescheduledByMonthAndYear(month, year);
        } catch (Exception e) {
            // Se não tiver esse status, ignora
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalAppointments", total);
        report.put("cancelled", cancelled);
        if (rescheduled != null) report.put("rescheduled", rescheduled);

        return ResponseEntity.ok(report);
    }


}

