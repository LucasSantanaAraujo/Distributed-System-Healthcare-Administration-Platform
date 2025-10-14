package com.psoft.clinic.controller;

import com.psoft.clinic.dto.AuthRequest;
import com.psoft.clinic.dto.AuthResponse;
import com.psoft.clinic.dto.PatientRegisterRequest;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.model.Role;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticação e registo de pacientes.
 *
 * ⚠️ Nesta versão (clients-service isolado), os endpoints mantêm o mesmo nome e formato,
 * mas as dependências relacionadas com autenticação global (JWT, AuthenticationManager)
 * estão temporariamente desativadas.
 */

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    // private final AuthenticationManager authenticationManager; // auth-service
    // private final PasswordEncoder passwordEncoder;             // auth-service
    // private final JwtUtil jwtUtil;                             // auth-service

    private boolean isPasswordValid(String password) {
        return password.length() >= 10 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }

    // ------------------------------------------------------------------------
    // 🔹 ENDPOINTS ORIGINAIS (mantidos mas simplificados)
    // ------------------------------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request,
                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email já está registado.");
        }

        if (!isPasswordValid(request.getPassword())) {
            return ResponseEntity.badRequest().body("Password inválida. Deve ter pelo menos 10 caracteres, incluindo uma maiúscula, um dígito e um caractere especial.");
        }

        Role roleToAssign = Role.PATIENT;

        // ⚠️ Aqui o JWT seria validado no auth-service.
        // Se recebermos tokens no futuro, será validado via HTTP entre serviços.

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword()) // 🔸 sem encriptação temporariamente
                .role(roleToAssign)
                .build();

        userRepository.save(user);

        // ⚠️ Geração de token desativada (pertence ao auth-service)
        // String token = jwtUtil.generateToken(user);
        // return ResponseEntity.ok(new AuthResponse(token));

        return ResponseEntity.ok(new AuthResponse("TOKEN_TEMPORARIO"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        // ⚠️ Autenticação desativada — pertence ao auth-service
        // authenticationManager.authenticate(...);

        var userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Credenciais inválidas.");
        }

        User user = userOpt.get();
        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body("Credenciais inválidas.");
        }

        // ⚠️ Geração de token removida (usará auth-service no futuro)
        return ResponseEntity.ok(new AuthResponse("TOKEN_TEMPORARIO"));
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
                .password(request.getPassword()) // 🔸 sem encriptação temporária
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

        user.setPatient(patient);

        userRepository.save(user);

        // ⚠️ Geração de token desativada (futura integração com auth-service)
        return ResponseEntity.ok(new AuthResponse("TOKEN_TEMPORARIO"));
    }
}
