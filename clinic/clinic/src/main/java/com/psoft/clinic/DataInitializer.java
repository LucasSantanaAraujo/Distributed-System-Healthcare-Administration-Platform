package com.psoft.clinic;

import com.psoft.clinic.model.*;
import com.psoft.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PhysicianRepository physicianRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentRecordRepository recordRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // --- ADMIN ---
            String adminEmail = "admin@clinica.pt";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = User.builder()
                        .name("Admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("Admin1234!"))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
            }

            // --- MÉDICOS & USERS ---
            List<String[]> medicoData = List.of(
                    new String[]{"Dr. Ana Silva", "Cardiologia", "912345670", "Seg-Sex 8h-14h"},
                    new String[]{"Dr. João Santos", "Neurologia", "912345671", "Ter-Qui 14h-20h"},
                    new String[]{"Dr. Marta Costa", "Pediatria", "912345672", "Seg-Qua 10h-16h"},
                    new String[]{"Dr. Pedro Ramos", "Dermatologia", "912345673", "Qua-Sex 16h-22h"},
                    new String[]{"Dr. Luís Martins", "Ortopedia", "912345674", "Seg-Sex 8h-12h"}
            );
            List<Physician> savedPhysicians = new java.util.ArrayList<>();
            for (int i = 0; i < medicoData.size(); i++) {
                String email = "medico" + (i + 1) + "@clinica.pt";
                if (userRepository.findByEmail(email).isEmpty()) {
                    User medicoUser = User.builder()
                            .email(email)
                            .password(passwordEncoder.encode("Medico1234!"))
                            .role(Role.PHYSICIAN)
                            .build();
                    userRepository.save(medicoUser);

                    Physician physician = Physician.builder()
                            .name(medicoData.get(i)[0])
                            .specialty(medicoData.get(i)[1])
                            .contact(medicoData.get(i)[2])
                            .schedule(medicoData.get(i)[3])
                            .user(medicoUser)
                            .build();
                    physicianRepository.save(physician);

                    medicoUser.setPhysician(physician);
                    userRepository.save(medicoUser);

                    savedPhysicians.add(physician);
                } else {
                    savedPhysicians.add(physicianRepository.findByUserEmail(email).orElseThrow());
                }
            }

            // --- PACIENTES & USERS ---
            List<String[]> pacienteData = List.of(
                    new String[]{"Maria Oliveira", "1990-05-10", "912345678", "Multicare"},
                    new String[]{"Tiago Mendes", "1985-02-20", "934567890", "AdvanceCare"},
                    new String[]{"Sofia Cardoso", "2000-11-22", "925678901", "Medis"}
            );
            List<Patient> savedPatients = new java.util.ArrayList<>();
            for (int i = 0; i < pacienteData.size(); i++) {
                String email = "paciente" + (i + 1) + "@clinica.pt";
                if (userRepository.findByEmail(email).isEmpty()) {
                    User pacienteUser = User.builder()
                            .email(email)
                            .password(passwordEncoder.encode("Paciente1234!"))
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
                    patientRepository.save(patient);

                    pacienteUser.setPatient(patient);
                    userRepository.save(pacienteUser);

                    savedPatients.add(patient);
                } else {
                    savedPatients.add(patientRepository.findByUserEmail(email).orElseThrow());
                }
            }

            // --- CONSULTAS (Appointments) ---
            List<Appointment> appointments = List.of(
                    Appointment.builder()
                            .dateTime(LocalDateTime.now().plusDays(1))
                            .reason("Check-up de Cardiologia")
                            .status(AppointmentStatus.CONFIRMED)
                            .patient(savedPatients.get(0))
                            .physician(savedPhysicians.get(0))
                            .build(),
                    Appointment.builder()
                            .dateTime(LocalDateTime.now().plusDays(2))
                            .reason("Consulta de Neurologia")
                            .status(AppointmentStatus.CONFIRMED)
                            .patient(savedPatients.get(1))
                            .physician(savedPhysicians.get(1))
                            .build(),
                    Appointment.builder()
                            .dateTime(LocalDateTime.now().minusDays(2))
                            .reason("Exame Dermatológico")
                            .status(AppointmentStatus.CONFIRMED)
                            .patient(savedPatients.get(2))
                            .physician(savedPhysicians.get(3))
                            .build(),
                    Appointment.builder()
                            .dateTime(LocalDateTime.now().minusDays(3))
                            .reason("Consulta de Ortopedia")
                            .status(AppointmentStatus.CONFIRMED)
                            .patient(savedPatients.get(1))
                            .physician(savedPhysicians.get(4))
                            .build()
            );
            appointmentRepository.saveAll(appointments);

            // --- REGISTOS DE CONSULTA (Só para as passadas) ---
            for (Appointment appt : appointments) {
                if (appt.getDateTime().isBefore(LocalDateTime.now())) {
                    AppointmentRecord record = AppointmentRecord.builder()
                            .appointment(appt)
                            .diagnosis("Diagnóstico exemplo para " + appt.getReason())
                            .treatment("Tratamento exemplo")
                            .prescription("Prescrição exemplo")
                            .build();
                    recordRepository.save(record);
                }
            }
        };
    }
}
