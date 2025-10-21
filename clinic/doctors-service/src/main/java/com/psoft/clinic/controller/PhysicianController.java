package com.psoft.clinic.controller;

import com.psoft.clinic.client.AppointmentsServiceClient;
import com.psoft.clinic.dto.PhysicianRequest;
import com.psoft.clinic.dto.PhysicianResponse;
import com.psoft.clinic.model.Physician;
import com.psoft.clinic.model.Role;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.UserRepository;
import com.psoft.clinic.repository.PhysicianRepository;
import com.psoft.clinic.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller dos médicos + funções administrativas.
 * Combina os endpoints originais de PhysicianController e AdminController.
 *
 * - CRUD de médicos
 * - Pesquisa e atualização
 * - Estatísticas administrativas (via appointments-service)
 */
@RestController
@EnableMethodSecurity(prePostEnabled = true)
@RequestMapping("/physicians")
@RequiredArgsConstructor
public class PhysicianController {

    private final PhysicianRepository physicianRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AppointmentsServiceClient appointmentsClient;
    private final AppointmentsServiceClient appointmentsServiceClient;// 👈 comunicação com appointments-service

    // ------------------------------------------------------------------------
    // 🔹 CRUD DE MÉDICOS
    // ------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPhysician(@RequestBody PhysicianRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email já registado.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PHYSICIAN)
                .build();

        Physician physician = Physician.builder()
                .name(request.getName())
                .specialty(request.getSpecialty())
                .contact(request.getContact())
                .schedule(request.getSchedule())
                .user(user)
                .build();

        user.setPhysician(physician);
        String token = jwtUtil.generateToken(user);
        user.setToken(token);

        userRepository.save(user);
        return ResponseEntity.ok("Médico criado com sucesso.");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PATIENT')")
    public ResponseEntity<?> getPhysicianById(@PathVariable Long id) {
        return physicianRepository.findById(id)
                .map(p -> PhysicianResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .specialty(p.getSpecialty())
                        .contact(p.getContact())
                        .schedule(p.getSchedule())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> searchPhysicians(@RequestParam("query") String query) {
        var physicians = physicianRepository.findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(query, query);
        var responses = physicians.stream().map(p -> PhysicianResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .specialty(p.getSpecialty())
                .contact(p.getContact())
                .schedule(p.getSchedule())
                .build()).toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePhysician(@PathVariable Long id, @RequestBody PhysicianRequest request) {
        return physicianRepository.findById(id)
                .map(physician -> {
                    physician.setName(request.getName());
                    physician.setSpecialty(request.getSpecialty());
                    physician.setContact(request.getContact());
                    physician.setSchedule(request.getSchedule());
                    physicianRepository.save(physician);
                    return ResponseEntity.ok("Dados do médico atualizados com sucesso.");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPhysicianWithPhoto(
            @RequestParam("name") String name,
            @RequestParam("specialty") String specialty,
            @RequestParam("contact") String contact,
            @RequestParam("schedule") String schedule,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        String photoPath = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                String uploadsDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
                File dir = new File(uploadsDir);
                if (!dir.exists()) dir.mkdirs();
                photoPath = uploadsDir + System.currentTimeMillis() + "_" + photo.getOriginalFilename();
                photo.transferTo(new File(photoPath));
            } catch (IOException e) {
                return ResponseEntity.status(500).body("Erro ao guardar foto: " + e.getMessage());
            }
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.PHYSICIAN)
                .build();

        Physician physician = Physician.builder()
                .name(name)
                .specialty(specialty)
                .contact(contact)
                .schedule(schedule)
                .photoPath(photoPath)
                .user(user)
                .build();

        user.setPhysician(physician);
        userRepository.save(user);

        return ResponseEntity.ok("Médico criado com sucesso.");
    }

    // ------------------------------------------------------------------------
    // 🔹 FUNÇÕES ADMINISTRATIVAS (integradas)
    // ------------------------------------------------------------------------

    @GetMapping("/admin/only")
    @PreAuthorize("hasRole('ADMIN')")
    public String onlyForAdmins() {
        return "Bem-vindo, ADMIN!";
    }

    @GetMapping("/admin/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTopPhysicians() {
        try {
            // 1️⃣ Pede os dados do outro serviço
            String jsonResponse = appointmentsServiceClient.getTopPhysiciansByAppointmentCount();

            // 2️⃣ Converte JSON em lista de mapas
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> stats = mapper.readValue(jsonResponse, new TypeReference<>() {});

            // 3️⃣ Busca todos os médicos locais
            List<Physician> physicians = physicianRepository.findAll();

            // 4️⃣ Combina os dados de ambos
            var response = stats.stream().map(stat -> {
                Long id = ((Number) stat.get("id")).longValue();
                Physician physician = physicians.stream()
                        .filter(p -> Objects.equals(p.getId(), id))
                        .findFirst()
                        .orElse(null);

                Map<String, Object> map = new HashMap<>();
                map.put("id", id);
                map.put("name", physician != null ? physician.getName() : "Desconhecido");
                map.put("specialty", physician != null ? physician.getSpecialty() : "-");
                map.put("appointments", stat.get("appointments"));
                return map;
            }).toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao obter dados: " + e.getMessage());
        }
    }

    @GetMapping("/admin/average-duration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAverageAppointmentDurationPerPhysician() {
        List<Physician> physicians = physicianRepository.findAll();
        Map<String, Integer> averages = new HashMap<>();
        for (Physician physician : physicians) {
            averages.put(physician.getName(), 30);
        }
        return ResponseEntity.ok(averages);
    }

    @GetMapping("/admin/monthly-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMonthlyReport(@RequestParam int year, @RequestParam int month) {
        try {
            String report = appointmentsServiceClient.getMonthlyReport(year, month);
            return ResponseEntity.ok(report); // ✅ devolve a string como corpo da resposta
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao obter relatório: " + e.getMessage());
        }
    }
}
