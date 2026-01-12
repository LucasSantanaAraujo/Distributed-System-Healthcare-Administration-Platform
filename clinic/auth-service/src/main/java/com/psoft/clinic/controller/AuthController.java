package com.psoft.clinic.controller;

import com.psoft.clinic.dto.AuthRequest;
import com.psoft.clinic.dto.AuthResponse;
import com.psoft.clinic.dto.PatientCreateRequest;
import com.psoft.clinic.dto.PhysicianCreateRequest;
import com.psoft.clinic.keycloak.KeycloakClient;
import com.psoft.clinic.keycloak.dto.KeycloakTokenResponse;
import com.psoft.clinic.model.Role;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final KeycloakClient keycloakClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.clients-service.url}")
    private String clientsServiceUrl;

    @Value("${app.doctors-service.url}")
    private String doctorsServiceUrl;

    @Value("${internal.api.key}")
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

    private String mapRoleToKeycloak(Role role) {
        // Ajusta aqui se no Keycloak os roles tiverem outro nome.
        // Pelo teu token tens "ADMIN" uppercase.
        return role.name(); // PATIENT / PHYSICIAN / ADMIN
    }

    // ----------------------------------------------------------
    // REGISTO (Keycloak + criar Patient no clients-service)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {

        if (!isPasswordValid(request.getPassword())) {
            return ResponseEntity.badRequest().body("Password inválida.");
        }

        Role roleToAssign = request.getRole() != null ? request.getRole() : Role.PATIENT;
        String kcRole = mapRoleToKeycloak(roleToAssign);

        try {
            // 1) Criar user no Keycloak (o ID devolvido é o UUID do Keycloak)
            String keycloakUserId = keycloakClient.createUser(
                    request.getEmail(),
                    request.getName(),
                    request.getPassword(),
                    kcRole
            );

            // 2) Se for patient: cria no clients-service via internal endpoint
            if (roleToAssign == Role.PATIENT) {
                PatientCreateRequest patientRequest = new PatientCreateRequest(
                        keycloakUserId,
                        request.getName(),
                        request.getBirthDate(),
                        request.getPhone(),
                        request.getInsuranceInfo(),
                        request.getConsent()
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

                logger.info("✅ Paciente criado no clients-service (userId Keycloak = {}).", keycloakUserId);
            }

            // 3) devolver token já válido (Keycloak)
            KeycloakTokenResponse token = keycloakClient.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(new AuthResponse(token.getAccessToken()));

        } catch (Exception e) {
            logger.error("❌ Erro no register: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erro ao registar utilizador: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // LOGIN (Keycloak token)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            KeycloakTokenResponse token = keycloakClient.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(new AuthResponse(token.getAccessToken()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Credenciais inválidas.");
        }
    }

    // ----------------------------------------------------------
    // REGISTO DE MÉDICO (Keycloak + doctors-service internal)
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

        if (!isPasswordValid(password)) {
            return ResponseEntity.badRequest().body("Password inválida.");
        }

        try {
            // 1) Guardar foto (opcional)
            String photoPath = null;
            if (photo != null && !photo.isEmpty()) {
                String uploadsDir = System.getProperty("user.dir") + File.separator + uploadDir + File.separator;
                File dir = new File(uploadsDir);
                if (!dir.exists()) dir.mkdirs();

                photoPath = uploadsDir + System.currentTimeMillis() + "_" + photo.getOriginalFilename();
                photo.transferTo(new File(photoPath));
                logger.info("📸 [registerPhysician] Foto guardada em {}", photoPath);
            }

            // 2) Criar user no Keycloak com role PHYSICIAN
            String keycloakUserId = keycloakClient.createUser(
                    email,
                    name,
                    password,
                    mapRoleToKeycloak(Role.PHYSICIAN)
            );

            // 3) Criar no doctors-service
            PhysicianCreateRequest physicianRequest = PhysicianCreateRequest.builder()
                    .userId(keycloakUserId)
                    .name(name)
                    .specialty(specialty)
                    .contact(contact)
                    .schedule(schedule)
                    .photoPath(photoPath)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PhysicianCreateRequest> entity = new HttpEntity<>(physicianRequest, headers);

            String url = doctorsServiceUrl + "/physicians/internal";
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
    }
}
