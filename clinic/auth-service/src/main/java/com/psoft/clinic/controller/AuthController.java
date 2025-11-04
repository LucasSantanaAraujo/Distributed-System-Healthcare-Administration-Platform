package com.psoft.clinic.controller;

import com.psoft.clinic.dto.AuthRequest;
import com.psoft.clinic.dto.AuthResponse;
import com.psoft.clinic.dto.PatientCreateRequest;
import com.psoft.clinic.model.Role;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.UserRepository;
import com.psoft.clinic.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.psoft.clinic.dto.PhysicianCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);


    // Configuração opcional via application.properties
    @Value("${app.clients-service.url:http://clients-service:8080}")
    private String clientsServiceUrl;

    @Value("${app.doctors-service.url}")
    private String doctorsServiceUrl;

    @Value("${app.internal-api-key:12345}")
    private String internalApiKey;
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    // ----------------------------------------------------------
    // Validação de password
    private boolean isPasswordValid(String password) {
        return password.length() >= 10 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }

    // ----------------------------------------------------------
    // REGISTO
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email já registado.");
        }

        if (!isPasswordValid(request.getPassword())) {
            return ResponseEntity.badRequest().body("Password inválida.");
        }

        Role roleToAssign = request.getRole() != null ? request.getRole() : Role.PATIENT;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(roleToAssign)
                .birthDate(request.getBirthDate())
                .phone(request.getPhone())
                .insuranceInfo(request.getInsuranceInfo())
                .consent(request.getConsent())
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        // ===============================================
        // Criação no serviço correto (só para pacientes)
        // ===============================================
        if (roleToAssign == Role.PATIENT) {
            PatientCreateRequest patientRequest = new PatientCreateRequest(
                    user.getId(),
                    user.getName(),
                    user.getBirthDate(),
                    user.getPhone(),
                    user.getInsuranceInfo(),
                    user.getConsent()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PatientCreateRequest> entity = new HttpEntity<>(patientRequest, headers);

            restTemplate.postForEntity(
                    clientsServiceUrl + "/patients/internal",
                    entity,
                    String.class
            );

            System.out.println("✅ Paciente criado no clients-service.");
        }

        return ResponseEntity.ok(new AuthResponse(token));
    }

    // ----------------------------------------------------------
    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Credenciais inválidas.");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user);
        user.setToken(token);
        userRepository.save(user);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    // ----------------------------------------------------------
    // REGISTO DE MÉDICO (com foto opcional)
    // REGISTO DE MÉDICO (com foto opcional)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/register/physician", consumes = "multipart/form-data")
    public ResponseEntity<?> registerPhysician(
            @RequestParam("name") String name,
            @RequestParam("specialty") String specialty,
            @RequestParam("contact") String contact,
            @RequestParam("schedule") String schedule,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        logger.info("➡️ [registerPhysician] Início do registo de médico com email: {}", email);

        try {
            // 1️⃣ Verificar duplicados
            if (userRepository.existsByEmail(email)) {
                logger.warn("⚠️ [registerPhysician] Tentativa de registo com email duplicado: {}", email);
                return ResponseEntity.badRequest().body("Email já registado.");
            }

            // 2️⃣ Guardar foto (opcional)
            String photoPath = null;
            if (photo != null && !photo.isEmpty()) {
                logger.debug("🖼️ [registerPhysician] Foto recebida: {}", photo.getOriginalFilename());

                String uploadsDir = System.getProperty("user.dir") + File.separator + uploadDir + File.separator;
                File dir = new File(uploadsDir);
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    logger.debug("📁 Diretório criado: {}", created);
                }

                photoPath = uploadsDir + System.currentTimeMillis() + "_" + photo.getOriginalFilename();
                photo.transferTo(new File(photoPath));
                logger.info("📸 [registerPhysician] Foto guardada em {}", photoPath);
            } else {
                logger.debug("ℹ️ [registerPhysician] Nenhuma foto enviada.");
            }

            // 3️⃣ Criar o User no auth-service
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(Role.PHYSICIAN)
                    .build();

            userRepository.save(user);
            logger.info("✅ [registerPhysician] User criado com ID {}", user.getId());

            // 4️⃣ Criar o PhysicianCreateRequest
            PhysicianCreateRequest physicianRequest = PhysicianCreateRequest.builder()
                    .userId(user.getId())
                    .name(name)
                    .specialty(specialty)
                    .contact(contact)
                    .schedule(schedule)
                    .photoPath(photoPath)
                    .build();

            logger.debug("📦 [registerPhysician] Dados enviados para doctors-service: {}", physicianRequest);

            // 5️⃣ Adicionar o header interno (X-INTERNAL-KEY)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", "12345"); // Usa o mesmo valor do application.properties
            HttpEntity<PhysicianCreateRequest> entity = new HttpEntity<>(physicianRequest, headers);

            String url = doctorsServiceUrl + "/physicians/internal";
            logger.info("🌐 [registerPhysician] A enviar POST para {}", url);

            // 6️⃣ Enviar o pedido
            String response = restTemplate.postForObject(url, entity, String.class);

            logger.info("📡 [registerPhysician] Resposta do doctors-service: {}", response);

            return ResponseEntity.ok("Médico registado com sucesso!");

        } catch (IOException e) {
            logger.error("❌ [registerPhysician] Erro ao guardar foto: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erro ao guardar foto: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ [registerPhysician] Erro geral: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erro ao registar médico: " + e.getMessage());
        }
    }  }
