package com.psoft.clinic;

import com.psoft.clinic.model.*;
import com.psoft.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Inicializa dados apenas para o doctors-service.
 * Cria médicos e consultas (appointments) de exemplo.
 *
 * ❗ Todas as partes relacionadas com utilizadores, pacientes e registos
 * foram removidas/comentadas, pois pertencem a outros microserviços.
 */

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final PhysicianRepository physicianRepository;
    private final AppointmentRepository appointmentRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {

            // --- MÉDICOS DE EXEMPLO ---
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
            }

            // --- CONSULTAS DE EXEMPLO ---
            if (appointmentRepository.count() == 0) {
                List<Physician> physicians = physicianRepository.findAll();

                if (!physicians.isEmpty()) {
                    List<Appointment> appointments = List.of(
                            Appointment.builder()
                                    .dateTime(LocalDateTime.now().plusDays(1))
                                    .reason("Check-up de Cardiologia")
                                    .status(AppointmentStatus.CONFIRMED)
                                    .patientId(1L) // apenas ID simbólico (paciente remoto)
                                    .physician(physicians.get(0))
                                    .build(),

                            Appointment.builder()
                                    .dateTime(LocalDateTime.now().plusDays(2))
                                    .reason("Consulta de Neurologia")
                                    .status(AppointmentStatus.CONFIRMED)
                                    .patientId(2L)
                                    .physician(physicians.get(1))
                                    .build(),

                            Appointment.builder()
                                    .dateTime(LocalDateTime.now().minusDays(1))
                                    .reason("Consulta de Pediatria")
                                    .status(AppointmentStatus.CONFIRMED)
                                    .patientId(3L)
                                    .physician(physicians.get(2))
                                    .build()
                    );
                    appointmentRepository.saveAll(appointments);
                }
            }

            System.out.println("✅ Médicos e consultas de exemplo carregados no doctors-service.");
        };
    }
}
