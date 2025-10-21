package com.psoft.clinic.util;

import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Inicializa dados de exemplo apenas para o appointments-service.
 * Este serviço apenas armazena IDs de paciente e médico.
 */
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AppointmentRepository appointmentRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (appointmentRepository.count() == 0) {
                List<Appointment> appointments = List.of(
                        Appointment.builder()
                                .dateTime(LocalDateTime.now().plusDays(1))
                                .reason("Check-up de Cardiologia")
                                .status(AppointmentStatus.CONFIRMED)
                                .patientId(1L)      // paciente remoto (clients-service)
                                .physicianId(1L)    // médico remoto (doctors-service)
                                .build(),

                        Appointment.builder()
                                .dateTime(LocalDateTime.now().plusDays(2))
                                .reason("Consulta de Neurologia")
                                .status(AppointmentStatus.CONFIRMED)
                                .patientId(2L)
                                .physicianId(2L)
                                .build(),

                        Appointment.builder()
                                .dateTime(LocalDateTime.now().minusDays(1))
                                .reason("Consulta de Pediatria")
                                .status(AppointmentStatus.CONFIRMED)
                                .patientId(3L)
                                .physicianId(3L)
                                .build()
                );

                appointmentRepository.saveAll(appointments);
                System.out.println("✅ Consultas de exemplo criadas no appointments-service.");
            } else {
                System.out.println("ℹ️ Consultas já existentes — nenhum dado novo criado.");
            }
        };
    }
}
