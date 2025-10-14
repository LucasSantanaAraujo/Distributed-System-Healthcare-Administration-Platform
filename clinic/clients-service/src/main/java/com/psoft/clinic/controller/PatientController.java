package com.psoft.clinic.controller;

import com.psoft.clinic.dto.PatientResponse;
import com.psoft.clinic.dto.PatientUpdateRequest;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.repository.PatientRepository;
import com.psoft.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador responsável pelos endpoints de pacientes.
 *
 * ⚙️ Mantém os mesmos endpoints originais (/patients, /patients/{id}, etc.)
 * mas sem autenticação JWT nesta fase.
 */

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    // ------------------------------------------------------------------------
    // 🔹 LISTAR TODOS OS PACIENTES
    // ------------------------------------------------------------------------
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

    // ------------------------------------------------------------------------
    // 🔹 OBTER PACIENTE POR ID
    // ------------------------------------------------------------------------
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
                .email(p.getUser() != null ? p.getUser().getEmail() : "(sem email)")
                .birthDate(p.getBirthDate())
                .phone(p.getContact())
                .insuranceInfo(p.getInsuranceInfo())
                .build();

        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------------
    // 🔹 PROCURAR PACIENTES POR NOME
    // ------------------------------------------------------------------------
    @GetMapping("/search")
    public ResponseEntity<?> searchPatientsByName(@RequestParam String name) {
        var matches = patientRepository.findByNameContainingIgnoreCase(name);

        var responses = matches.stream().map(p ->
                PatientResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .email(p.getUser() != null ? p.getUser().getEmail() : "(sem email)")
                        .birthDate(p.getBirthDate())
                        .phone(p.getContact())
                        .insuranceInfo(p.getInsuranceInfo())
                        .build()
        ).toList();

        return ResponseEntity.ok(responses);
    }

    // ------------------------------------------------------------------------
    // 🔹 ATUALIZAR DADOS DO PACIENTE
    // ------------------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMyData(@PathVariable Long id,
                                          @RequestBody PatientUpdateRequest request) {
        var patientOpt = patientRepository.findById(id);
        if (patientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Patient patient = patientOpt.get();

        // ⚠️ No futuro será validado com JWT (auth-service)
        // Por agora, apenas atualizamos os dados diretamente.

        patient.setName(request.getName());
        patient.setContact(request.getPhone());
        patient.setInsuranceInfo(request.getInsuranceInfo());
        patient.setPhotoBase64(request.getPhotoBase64());
        patient.setHealthConcerns(request.getHealthConcerns());

        patientRepository.save(patient);

        return ResponseEntity.ok("Patient updated successfully");
    }
}
