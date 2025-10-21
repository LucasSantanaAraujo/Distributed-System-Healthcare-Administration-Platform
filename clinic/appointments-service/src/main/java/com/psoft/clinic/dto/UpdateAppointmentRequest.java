package com.psoft.clinic.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAppointmentRequest {
    private LocalDateTime dateTime;
    private String reason;
    private boolean cancel; // se for true, cancela a consulta
}
