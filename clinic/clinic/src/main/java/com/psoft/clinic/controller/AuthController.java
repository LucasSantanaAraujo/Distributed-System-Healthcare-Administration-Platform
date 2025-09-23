package com.psoft.clinic.controller;

import com.psoft.clinic.dto.AuthRequest;
import com.psoft.clinic.dto.AuthResponse;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.model.Role;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.UserRepository;
import com.psoft.clinic.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.AuthenticationException;
import com.psoft.clinic.dto.PatientRegisterRequest;



@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private boolean isPasswordValid(String password) {
        return password.length() >= 10 &&
                password.matches(".*[A-Z].*") &&    // pelo menos uma maiúscula
                password.matches(".*\\d.*") &&     // pelo menos um dígito
                password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*"); // especial
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request,
                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email já está registado.");
        }

        // ⚠️ Validação da password
        if (!isPasswordValid(request.getPassword())) {
            return ResponseEntity.badRequest().body("Password inválida. Deve ter pelo menos 10 caracteres, incluindo uma maiúscula, um dígito e um caractere especial.");
        }

        Role roleToAssign = Role.PATIENT;

        if (request.getRole() != null && request.getRole() != Role.PATIENT) {
            // Se o user quer criar algo diferente de PATIENT, precisa de autorização JWT
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(403).body("Não autorizado a criar utilizadores com esse papel.");
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.getEmailFromToken(token);

            User requester = userRepository.findByEmail(email).orElse(null);
            if (requester == null || requester.getRole() != Role.ADMIN) {
                return ResponseEntity.status(403).body("Apenas administradores podem criar utilizadores com esse papel.");
            }

            roleToAssign = request.getRole();
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(roleToAssign)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            System.out.println("LOGIN BEM-SUCEDIDO: " + request.getEmail());

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Credenciais inválidas.");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user);
        user.setToken(token);
        userRepository.save(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }
    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@RequestBody PatientRegisterRequest request) {
        System.out.println("✅ ENTROU NO /register/patient");

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email já está registado.");
        }
        if (!isPasswordValid(request.getPassword())) {
            return ResponseEntity.badRequest().body("Password inválida. Deve ter pelo menos 10 caracteres, incluindo uma maiúscula, um dígito e um caractere especial.");
        }

        // Criar utilizador base
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PATIENT)
                .build();

        // Criar paciente associado
        Patient patient = Patient.builder()
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .contact(request.getPhone())
                .insuranceInfo(request.getInsuranceInfo())
                .consent(request.isConsent())
                .user(user)
                .build();

        user.setPatient(patient); // ligação bidirecional

        userRepository.save(user); // salva ambos por cascade

        String token = jwtUtil.generateToken(user);
        user.setToken(token);
        userRepository.save(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }

}
