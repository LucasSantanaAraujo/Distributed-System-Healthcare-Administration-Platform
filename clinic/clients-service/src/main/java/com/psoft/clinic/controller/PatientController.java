    package com.psoft.clinic.controller;

    import com.psoft.clinic.dto.PatientCreateRequest;
    import com.psoft.clinic.dto.PatientResponse;
    import com.psoft.clinic.dto.PatientUpdateRequest;
    import com.psoft.clinic.model.Patient;
    import com.psoft.clinic.repository.PatientRepository;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.web.bind.annotation.*;

    import java.security.Principal;
    import java.util.List;
    import java.util.Optional;

    @Slf4j
    @RestController
    @RequestMapping("/patients")
    @RequiredArgsConstructor
    public class PatientController {

        private final PatientRepository patientRepository;

        @Value("${internal.api.key}")
        private String internalApiKey;

        // ------------------------------------------------------------------------
        // 🔹 ENDPOINT INTERNO — usado pelo auth-service
        // ------------------------------------------------------------------------
        @PostMapping("/internal")
        public ResponseEntity<?> createPatientInternal(@RequestBody PatientCreateRequest request) {
            Optional<Patient> existingPatient = patientRepository.findByUserId(request.getUserId());
            if (existingPatient.isPresent()) {
                return ResponseEntity.ok("Paciente já existente.");
            }

            Patient patient = new Patient();
            patient.setUserId(request.getUserId());
            patient.setName(request.getName());
            patient.setBirthDate(request.getBirthDate());
            patient.setContact(request.getPhone());
            patient.setInsuranceInfo(request.getInsuranceInfo());
            patient.setConsent(request.getConsent());

            patientRepository.save(patient);
            System.out.println("💾 [createPatientInternal] Paciente guardado na BD: " + patient.getId());
            return ResponseEntity.ok("Paciente criado com sucesso!");
        }

        // ------------------------------------------------------------------------
    // 🔹 ENDPOINT INTERNO — usado pelo appointments-service
    // ------------------------------------------------------------------------
        @GetMapping("/{id}")
        public ResponseEntity<?> getPatientById(
                @PathVariable Long id,
                @RequestHeader(value = "X-INTERNAL-KEY", required = false) String internalKey,
                Principal principal) {

            boolean isInternal = internalKey != null && internalKey.equals(internalApiKey);

            // Se não for interno nem tiver role ADMIN, bloqueia
            if (!isInternal && (principal == null)) {
                return ResponseEntity.status(403).body("Acesso negado — requer autenticação ou chave interna.");
            }

            Optional<Patient> opt = patientRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body("Paciente não encontrado.");
            }

            Patient p = opt.get();

            // Se for interno, devolve o objeto completo (sem filtros)
            if (isInternal) {
                return ResponseEntity.ok(p);
            }

            // Caso contrário, devolve DTO (para uso externo)
            PatientResponse response = PatientResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .email("user-" + p.getUserId() + "@unknown")
                    .birthDate(p.getBirthDate())
                    .phone(p.getContact())
                    .insuranceInfo(p.getInsuranceInfo())
                    .build();

            return ResponseEntity.ok(response);
        }

        // ------------------------------------------------------------------------
        // 🔹 ENDPOINTS ADMINISTRATIVOS
        // ------------------------------------------------------------------------
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping
        public ResponseEntity<List<PatientResponse>> getAllPatients() {
            var patients = patientRepository.findAll().stream()
                    .map(p -> PatientResponse.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .email("user-" + p.getUserId() + "@unknown")
                            .birthDate(p.getBirthDate())
                            .phone(p.getContact())
                            .insuranceInfo(p.getInsuranceInfo())
                            .build()
                    ).toList();

            return ResponseEntity.ok(patients);
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/search")
        public ResponseEntity<?> searchPatientsByName(@RequestParam String name) {
            var matches = patientRepository.findByNameContainingIgnoreCase(name);

            var responses = matches.stream().map(p ->
                    PatientResponse.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .email("user-" + p.getUserId() + "@unknown")
                            .birthDate(p.getBirthDate())
                            .phone(p.getContact())
                            .insuranceInfo(p.getInsuranceInfo())
                            .build()
            ).toList();

            return ResponseEntity.ok(responses);
        }

        // ------------------------------------------------------------------------
        // 🔹 ATUALIZAÇÃO DE PERFIL (opcional)
        // ------------------------------------------------------------------------
        @PreAuthorize("hasRole('PATIENT')")
        @PutMapping("/{id}")
        public ResponseEntity<?> updateMyData(
                @PathVariable Long id,
                @RequestBody PatientUpdateRequest request) {

            var patientOpt = patientRepository.findById(id);
            if (patientOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Patient patient = patientOpt.get();
            patient.setName(request.getName());
            patient.setContact(request.getPhone());
            patient.setInsuranceInfo(request.getInsuranceInfo());
            patient.setPhotoBase64(request.getPhotoBase64());
            if (request.getHealthConcerns() != null) {
                patient.setHealthConcerns(String.join(", ", request.getHealthConcerns()));
            }
            patientRepository.save(patient);

            return ResponseEntity.ok("Dados do paciente atualizados com sucesso.");
        }
    }
