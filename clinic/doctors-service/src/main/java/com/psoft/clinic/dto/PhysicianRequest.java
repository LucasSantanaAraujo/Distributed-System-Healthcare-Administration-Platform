package com.psoft.clinic.dto;

import lombok.Data;

@Data
public class PhysicianRequest {
    private String name;
    private String specialty;
    private String contact;
    private String schedule;

    private String email;
    private String password;
}
