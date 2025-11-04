package com.psoft.clinic.dto;

import com.psoft.clinic.model.Role;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AuthRequest {
    private String name;
    private String email;
    private String password;
    private Role role;
    private LocalDate birthDate;
    private String phone;
    private String insuranceInfo;
    private Boolean consent;
    private String specialty;

}

