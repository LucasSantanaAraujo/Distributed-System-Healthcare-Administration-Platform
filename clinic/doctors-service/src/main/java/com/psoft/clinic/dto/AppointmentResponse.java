package com.psoft.clinic.dto;

import com.psoft.clinic.model.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private LocalDateTime dateTime;
    private String reason;
    private AppointmentStatus status;
    private String patientName;
    private String physicianName;
    private String diagnosis;
    private String treatment;
    private String prescription;

}
