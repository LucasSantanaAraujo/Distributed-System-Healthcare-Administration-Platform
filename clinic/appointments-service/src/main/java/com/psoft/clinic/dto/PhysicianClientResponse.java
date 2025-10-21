package com.psoft.clinic.dto;

import lombok.Data;

@Data
public class PhysicianClientResponse {
    private Long id;
    private String name;
    private String specialty;
    private String contact;
    private String schedule;
}
