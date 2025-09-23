package com.psoft.clinic.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PatientRegisterRequest {
    private String name;
    private String email;
    private String password;
    private LocalDate birthDate;
    private String phone;
    private String insuranceInfo;
    private boolean consent;
    private String photoBase64;
    private List<String> healthConcerns;
}
