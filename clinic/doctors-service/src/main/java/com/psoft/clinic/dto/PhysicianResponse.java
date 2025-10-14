package com.psoft.clinic.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PhysicianResponse {
    private Long id;
    private String name;
    private String specialty;
    private String contact;
    private String schedule;
}
