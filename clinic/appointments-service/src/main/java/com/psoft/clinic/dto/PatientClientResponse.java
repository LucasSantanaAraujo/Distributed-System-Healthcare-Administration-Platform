package com.psoft.clinic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientClientResponse {
    private Long id;
    private Long userId;
    private String name;
    private LocalDate birthDate;
    private String contact;
    private String insuranceInfo;
    private Boolean consent;
    private String photoBase64;
    private String healthConcerns;
}

