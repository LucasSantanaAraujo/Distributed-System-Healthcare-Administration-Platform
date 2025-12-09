package com.psoft.clinic;

import com.psoft.clinic.model.Patient;
import com.psoft.clinic.repository.PatientRepository;
import com.psoft.clinic.service.SequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final PatientRepository patientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Bean
    public CommandLineRunner initData() {
        return args -> {

            // --- Pacientes com os mesmos IDs do auth-service ---
            // ⚠️ Estes IDs DEVEM corresponder aos IDs reais do auth-service
            List<Object[]> pacientes = List.of(
                    new Object[]{2L, "Maria Oliveira", LocalDate.of(1990, 5, 10), "912345678", "Multicare"},
                    new Object[]{3L, "Tiago Mendes", LocalDate.of(1985, 2, 20), "934567890", "AdvanceCare"},
                    new Object[]{4L, "Sofia Cardoso", LocalDate.of(2000, 11, 22), "925678901", "Medis"}
            );

            for (Object[] p : pacientes) {
                Long userId = (Long) p[0];

                if (patientRepository.findByUserId(userId).isEmpty()) {
                    Patient patient = Patient.builder()
                            .id(sequenceGeneratorService.generateSequence("patients_sequence"))
                            .userId(userId)
                            .name((String) p[1])
                            .birthDate((LocalDate) p[2])
                            .contact((String) p[3])
                            .insuranceInfo((String) p[4])
                            .consent(true)
                            .build();


                    patientRepository.save(patient);
                    System.out.println("✅ Paciente criado no clients-service: " + p[1]);
                } else {
                    System.out.println("ℹ️ Paciente já existente: " + p[1]);
                }
            }

            System.out.println("✅ Dados coerentes criados no clients-service.");
        };
    }
}
