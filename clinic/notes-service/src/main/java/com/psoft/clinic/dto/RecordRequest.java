package com.psoft.clinic.dto;

import lombok.Data;

@Data
public class RecordRequest {
    private Long appointmentId;
    private String diagnosis;
    private String treatment;
    private String prescription;
}
