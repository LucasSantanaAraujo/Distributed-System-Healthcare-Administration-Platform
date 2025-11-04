package com.psoft.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianCreateRequest {
    private Long userId;
    private String name;
    private String specialty;
    private String contact;
    private String schedule;
    private String photoPath;
}
