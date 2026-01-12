package com.psoft.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PatientCreateRequest {
    private String userId;
    private String name;
    private LocalDate birthDate;
    private String phone;
    private String insuranceInfo;
    private boolean consent;

}
