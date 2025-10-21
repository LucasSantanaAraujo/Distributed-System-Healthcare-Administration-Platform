package com.psoft.clinic.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientAppointmentRequest {
    private Long patientId;          // vem do clients-service (ID do paciente)
    private Long physicianId;        // vem do doctors-service (ID do médico)
    private LocalDateTime dateTime;  // data/hora pretendida
    private String consultationType; // ex.: "Consulta Geral", "Revisão", etc.
}
