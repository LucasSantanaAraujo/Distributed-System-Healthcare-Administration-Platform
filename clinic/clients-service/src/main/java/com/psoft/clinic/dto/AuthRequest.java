package com.psoft.clinic.dto;

import com.psoft.clinic.model.Role;
import lombok.Data;

@Data
public class AuthRequest {
    private String name; // no login pode ser ignorado
    private String email;
    private String password;
    private Role role;

}
