package com.psoft.clinic;

import com.psoft.clinic.model.Role;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Verifica se já existe um admin
            if (userRepository.findByEmail("admin@clinica.pt").isEmpty()) {
                User admin = User.builder()
                        .name("Administrador")
                        .email("admin@clinica.pt")
                        .password(passwordEncoder.encode("Admin1234!"))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("✅ Admin criado: admin@clinica.pt / Admin1234!");
            } else {
                System.out.println("ℹ️ Admin já existente, não foi recriado.");
            }
        };
    }
}
