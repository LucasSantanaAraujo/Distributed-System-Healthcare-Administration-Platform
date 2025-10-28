package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateTime;
    private String reason;

    private Long patientId;
    private Long physicianId;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @Column(name = "patient_birth_date")
    private LocalDate patientBirthDate;

    @Column(name = "record_created_at")
    private LocalDateTime recordCreatedAt;

    public LocalDate getPatientBirthDate() {
        return patientBirthDate;
    }

    public LocalDateTime getRecordCreatedAt() {
        return recordCreatedAt;
    }
}
