package com.psoft.clinic.dto;

import lombok.Data;

@Data
public class AppointmentDetailsRequest {
    private String diagnosis;
    private String treatment;
    private String prescription;
}
