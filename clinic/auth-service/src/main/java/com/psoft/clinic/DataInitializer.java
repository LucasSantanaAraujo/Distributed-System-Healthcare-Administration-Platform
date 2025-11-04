package com.psoft.clinic;

import com.psoft.clinic.model.Role;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {

            // --- ADMIN ---
            String adminEmail = "admin@clinica.pt";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = User.builder()
                        .name("Administrador")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("Admin1234!"))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("✅ Admin criado: " + adminEmail);
            }

            // --- PACIENTES (os mesmos que no clients-service) ---
            List<User> pacientes = List.of(
                    User.builder()
                            .name("Maria Oliveira")
                            .email("paciente1@clinica.pt")
                            .password(passwordEncoder.encode("Paciente1234!"))
                            .role(Role.PATIENT)
                            .birthDate(LocalDate.of(1990, 5, 10))
                            .phone("912345678")
                            .insuranceInfo("Multicare")
                            .consent(true)
                            .build(),

                    User.builder()
                            .name("Tiago Mendes")
                            .email("paciente2@clinica.pt")
                            .password(passwordEncoder.encode("Paciente1234!"))
                            .role(Role.PATIENT)
                            .birthDate(LocalDate.of(1985, 2, 20))
                            .phone("934567890")
                            .insuranceInfo("AdvanceCare")
                            .consent(true)
                            .build(),

                    User.builder()
                            .name("Sofia Cardoso")
                            .email("paciente3@clinica.pt")
                            .password(passwordEncoder.encode("Paciente1234!"))
                            .role(Role.PATIENT)
                            .birthDate(LocalDate.of(2000, 11, 22))
                            .phone("925678901")
                            .insuranceInfo("Medis")
                            .consent(true)
                            .build()
            );

            for (User u : pacientes) {
                if (userRepository.findByEmail(u.getEmail()).isEmpty()) {
                    userRepository.save(u);
                    System.out.println("✅ Paciente criado: " + u.getEmail());
                } else {
                    System.out.println("ℹ️ Paciente já existente: " + u.getEmail());
                }
            }
        };
    }
}
