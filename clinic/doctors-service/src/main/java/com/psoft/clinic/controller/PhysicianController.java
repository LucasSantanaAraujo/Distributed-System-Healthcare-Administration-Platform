package com.psoft.clinic.controller;
import lombok.extern.slf4j.Slf4j;

import com.psoft.clinic.client.AppointmentsServiceClient;
import com.psoft.clinic.dto.PhysicianCreateRequest;
import com.psoft.clinic.dto.PhysicianRequest;
import com.psoft.clinic.dto.PhysicianResponse;
import com.psoft.clinic.model.Physician;
import com.psoft.clinic.model.Role;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.UserRepository;
import com.psoft.clinic.repository.PhysicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.security.Principal;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**

 * Controller dos médicos + funções administrativas.
 * O AUTH-SERVICE é responsável por gerar tokens JWT.
 * Este serviço apenas valida tokens recebidos.
 */
@Slf4j
@RestController
@EnableMethodSecurity(prePostEnabled = true)
@RequestMapping("/physicians")
@RequiredArgsConstructor
public class PhysicianController {

    private final PhysicianRepository physicianRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppointmentsServiceClient appointmentsServiceClient;
    @Value("${internal.api.key}")
    private String internalApiKey;


    // ------------------------------------------------------------------------
    // 🔹 CRUD DE MÉDICOS
    // ------------------------------------------------------------------------



    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPhysician(@RequestBody PhysicianRequest request) {
        // 1️⃣ Impede e-mails duplicados no User
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email já registado.");
        }

        // 2️⃣ Cria e guarda o utilizador com role PHYSICIAN
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PHYSICIAN)
                .build();

        userRepository.save(user); // 💾 importante!

        // 3️⃣ Cria o médico associado ao utilizador
        Physician physician = Physician.builder()
                .name(request.getName())
                .specialty(request.getSpecialty())
                .contact(request.getContact())
                .schedule(request.getSchedule())
                .userId(user.getId()) // agora já tem ID real
                .build();

        try {
            physicianRepository.save(physician);
            log.info("💾 Médico guardado na BD: {}", physician.getId());
            return ResponseEntity.ok("Médico criado com sucesso!");
        } catch (DataIntegrityViolationException e) {
            log.warn("⚠️ Tentativa de criar médico duplicado para userId {}", user.getId());
            return ResponseEntity.status(409).body("Médico já existe (violação de integridade).");
        } catch (Exception e) {
            log.error("❌ Erro ao guardar médico: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erro interno ao criar médico.");
        }
    }



    @PostMapping("/internal")
    public ResponseEntity<?> createPhysicianInternal(@RequestBody PhysicianCreateRequest request) {
        // Verificar se o médico já existe baseado no userId
        Optional<Physician> existingPhysician = physicianRepository.findByUserId(request.getUserId());
        if (existingPhysician.isPresent()) {
            return ResponseEntity.ok("Médico já existente.");
        }

        // Criar novo médico
        Physician physician = new Physician();
        physician.setUserId(request.getUserId());
        physician.setName(request.getName());
        physician.setSpecialty(request.getSpecialty());
        physician.setContact(request.getContact());
        physician.setSchedule(request.getSchedule());
        physician.setPhotoPath(request.getPhotoPath()); // Se tiver foto

        // Salvar o médico na base de dados
        physicianRepository.save(physician);
        System.out.println("💾 [createPhysicianInternal] Médico guardado na BD: " + physician.getId());

        return ResponseEntity.ok("Médico criado com sucesso!");
    }





    // ------------------------------------------------------------------------
// 🔹 ENDPOINT INTERNO — usado pelo appointments-service
// ------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<?> getPhysicianById(

            @PathVariable Long id,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String internalKey,
            Principal principal) {

        boolean isInternal = internalKey != null && internalKey.equals(internalApiKey);

        // Se não for pedido interno nem tiver autenticação, bloqueia
        if (!isInternal && principal == null) {
            return ResponseEntity.status(403)
                    .body("Acesso negado — requer autenticação ou chave interna.");
        }

        return physicianRepository.findById(id)
                .map(p -> {
                    // Se for pedido interno → devolve todos os dados
                    if (isInternal) {
                        return ResponseEntity.ok(p);
                    }

                    // Caso contrário (pedido autenticado com JWT) → devolve DTO limitado
                    PhysicianResponse dto = PhysicianResponse.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .specialty(p.getSpecialty())
                            .contact(p.getContact())
                            .schedule(p.getSchedule())
                            .build();
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.status(404).body("Médico não encontrado."));
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
                .userId(user.getId())
                .build();

        physicianRepository.save(physician);

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
            String jsonResponse = appointmentsServiceClient.getTopPhysiciansByAppointmentCount();
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> stats = mapper.readValue(jsonResponse, new TypeReference<>() {});

            List<Physician> physicians = physicianRepository.findAll();

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
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao obter relatório: " + e.getMessage());
        }
    }
}
