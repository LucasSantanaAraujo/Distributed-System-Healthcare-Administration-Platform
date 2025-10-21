package com.psoft.clinic.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentRequest {
    private LocalDateTime dateTime;
    private String reason;
    private Long physicianId;
}
