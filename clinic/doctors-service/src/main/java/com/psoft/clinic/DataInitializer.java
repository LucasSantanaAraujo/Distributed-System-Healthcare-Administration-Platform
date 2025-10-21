package com.psoft.clinic;

import com.psoft.clinic.model.Physician;
import com.psoft.clinic.repository.PhysicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false) // 🔧 evita o erro de CGLIB
@RequiredArgsConstructor
public class DataInitializer {

    private final PhysicianRepository physicianRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {

            if (physicianRepository.count() == 0) {
                List<Physician> physicians = List.of(
                        Physician.builder()
                                .name("Dr. Ana Silva")
                                .specialty("Cardiologia")
                                .contact("912345670")
                                .schedule("Seg-Sex 8h-14h")
                                .build(),
                        Physician.builder()
                                .name("Dr. João Santos")
                                .specialty("Neurologia")
                                .contact("912345671")
                                .schedule("Ter-Qui 14h-20h")
                                .build(),
                        Physician.builder()
                                .name("Dr. Marta Costa")
                                .specialty("Pediatria")
                                .contact("912345672")
                                .schedule("Seg-Qua 10h-16h")
                                .build()
                );
                physicianRepository.saveAll(physicians);
                System.out.println("✅ Médicos de exemplo carregados no doctors-service.");
            }
        };
    }
}
