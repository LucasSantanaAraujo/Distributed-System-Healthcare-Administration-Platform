package com.psoft.clinic.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAppointmentRequest {
    private Long patientId;
    private Long physicianId;
    private LocalDateTime dateTime;
    private String reason; // tipo de consulta
}
