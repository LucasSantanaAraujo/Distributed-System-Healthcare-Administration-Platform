package com.psoft.clinic.controller;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/physicians")
@RequiredArgsConstructor
public class    PhysicianController {

    private final PhysicianRepository physicianRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;// <- ADICIONADO

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPhysician(@RequestBody PhysicianRequest request) {

        // Criar o utilizador associado ao médico
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PHYSICIAN)
                .build();

        // Criar o médico e associar ao user
        Physician physician = Physician.builder()
                .name(request.getName())
                .specialty(request.getSpecialty())
                .contact(request.getContact())
                .schedule(request.getSchedule())
                .user(user)
                .build();

        user.setPhysician(physician);
        String token = jwtUtil.generateToken(user);
        user.setToken(token);// ligação bidirecional

        userRepository.save(user); // salva ambos via cascade


        return ResponseEntity.ok("Médico criado com sucesso.");
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'PATIENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPhysicianById(@PathVariable Long id) {
        return physicianRepository.findById(id)
                .map(physician -> PhysicianResponse.builder()
                        .id(physician.getId())
                        .name(physician.getName())
                        .specialty(physician.getSpecialty())
                        .contact(physician.getContact())
                        .schedule(physician.getSchedule())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/search")
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
            String uploadsDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
            File dir = new File(uploadsDir);
            if (!dir.exists()) dir.mkdirs();
            photoPath = uploadsDir + System.currentTimeMillis() + "_" + photo.getOriginalFilename();
            File dest = new File(photoPath);
            try {
                photo.transferTo(dest);
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



}
