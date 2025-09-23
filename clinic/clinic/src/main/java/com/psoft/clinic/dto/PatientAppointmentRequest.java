package com.psoft.clinic.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientAppointmentRequest {
    private Long physicianId;
    private LocalDateTime dateTime;
    private String consultationType;
}
