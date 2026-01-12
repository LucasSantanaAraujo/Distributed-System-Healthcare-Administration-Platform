package com.psoft.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianCreateRequest {
    private String userId;
    private String name;
    private String specialty;
    private String contact;
    private String schedule;
    private String photoPath;
}
