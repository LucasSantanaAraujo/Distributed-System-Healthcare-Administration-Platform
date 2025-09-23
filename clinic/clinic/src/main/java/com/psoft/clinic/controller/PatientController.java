package com.psoft.clinic.controller;

import com.psoft.clinic.dto.PatientRegisterRequest;
import com.psoft.clinic.dto.PatientResponse;
import com.psoft.clinic.dto.PatientUpdateRequest;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.PatientRepository;
import com.psoft.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        var patients = userRepository.findAll().stream()
                .filter(user -> user.getPatient() != null)
                .map(user -> PatientResponse.builder()
                        .id(user.getPatient().getId())
                        .name(user.getPatient().getName())
                        .email(user.getEmail())
                        .birthDate(user.getPatient().getBirthDate())
                        .phone(user.getPatient().getContact())
                        .insuranceInfo(user.getPatient().getInsuranceInfo())
                        .build()
                ).toList();

        return ResponseEntity.ok(patients);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPatientById(@PathVariable Long id) {
        var patientOpt = patientRepository.findById(id);
        if (patientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var p = patientOpt.get();

        PatientResponse response = PatientResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .email(p.getUser().getEmail())
                .birthDate(p.getBirthDate())
                .phone(p.getContact())
                .insuranceInfo(p.getInsuranceInfo())
                .build();

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<?> searchPatientsByName(@RequestParam String name) {
        var matches = patientRepository.findByNameContainingIgnoreCase(name);

        var responses = matches.stream().map(p ->
                PatientResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .email(p.getUser().getEmail())
                        .birthDate(p.getBirthDate())
                        .phone(p.getContact())
                        .insuranceInfo(p.getInsuranceInfo())
                        .build()
        ).toList();

        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMyData(@PathVariable Long id,
                                          @RequestBody PatientUpdateRequest request,
                                          Principal principal) {
        var patientOpt = patientRepository.findById(id);
        if (patientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Patient patient = patientOpt.get();

        // Buscar o user autenticado pelo email extraído do JWT
        var loggedUserOpt = userRepository.findByEmail(principal.getName());
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        var loggedUser = loggedUserOpt.get();

        // Verifica se o utilizador autenticado é o dono do paciente
        if (!patient.getUser().getId().equals(loggedUser.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        patient.setName(request.getName());
        patient.setContact(request.getPhone());
        patient.setInsuranceInfo(request.getInsuranceInfo());
        patient.setPhotoBase64(request.getPhotoBase64());
        patient.setHealthConcerns(request.getHealthConcerns());

        patientRepository.save(patient);

        return ResponseEntity.ok("Patient updated successfully");
    }
}
