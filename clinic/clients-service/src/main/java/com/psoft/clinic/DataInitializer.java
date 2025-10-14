package com.psoft.clinic;

import com.psoft.clinic.model.*;
import com.psoft.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

/**
 * Inicializador de dados apenas para o clients-service.
 * Cria alguns utilizadores e pacientes de exemplo.
 *
 * ❗ Todas as partes ligadas a médicos, consultas e registos foram removidas.
 */
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {

            // --- ADMIN ---
            String adminEmail = "admin@clinica.pt";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = User.builder()
                        .name("Admin")
                        .email(adminEmail)
                        .password("Admin1234!") // ⚠️ sem encriptação temporária
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
            }

            // --- PACIENTES & USERS ---
            List<String[]> pacienteData = List.of(
                    new String[]{"Maria Oliveira", "1990-05-10", "912345678", "Multicare"},
                    new String[]{"Tiago Mendes", "1985-02-20", "934567890", "AdvanceCare"},
                    new String[]{"Sofia Cardoso", "2000-11-22", "925678901", "Medis"}
            );

            for (int i = 0; i < pacienteData.size(); i++) {
                String email = "paciente" + (i + 1) + "@clinica.pt";
                if (userRepository.findByEmail(email).isEmpty()) {
                    User pacienteUser = User.builder()
                            .email(email)
                            .password("Paciente1234!") // ⚠️ sem encriptação temporária
                            .role(Role.PATIENT)
                            .build();
                    userRepository.save(pacienteUser);

                    Patient patient = Patient.builder()
                            .name(pacienteData.get(i)[0])
                            .birthDate(LocalDate.parse(pacienteData.get(i)[1]))
                            .contact(pacienteData.get(i)[2])
                            .insuranceInfo(pacienteData.get(i)[3])
                            .consent(true)
                            .user(pacienteUser)
                            .build();

                    pacienteUser.setPatient(patient);
                    patientRepository.save(patient);
                    userRepository.save(pacienteUser);
                }
            }

            System.out.println("✅ Pacientes e utilizadores de exemplo carregados no clients-service.");
        };
    }
}
