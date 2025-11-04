package com.psoft.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentBasicResponse {
    private Long id;
    private Long patientId;
    private Long physicianId;
    private String status;
    private String reason;
    private String dateTime; // podes usar LocalDateTime se quiseres
}
