package com.psoft.clinic.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PatientResponse {
    private Long id;
    private String name;
    private String email;
    private LocalDate birthDate;
    private String phone;
    private String insuranceInfo;
    private boolean consent;
}
