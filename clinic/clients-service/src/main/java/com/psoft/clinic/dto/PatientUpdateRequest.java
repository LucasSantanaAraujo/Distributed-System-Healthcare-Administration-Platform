package com.psoft.clinic.dto;

import lombok.Data;

import java.util.List;

@Data
public class PatientUpdateRequest {
    private String name;
    private String phone;
    private String insuranceInfo;
    private String photoBase64;
    private List<String> healthConcerns;
}
