package com.psoft.clinic.dto;


import lombok.Data;

@Data
public class PatientClientResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String insuranceInfo;
}
