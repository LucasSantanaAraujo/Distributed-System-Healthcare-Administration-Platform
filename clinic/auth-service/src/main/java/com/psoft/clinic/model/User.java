package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    // Campos adicionais
    private LocalDate birthDate;
    private String phone;
    private String insuranceInfo;
    private Boolean consent;

    // Token para autenticação
    @Column(length = 1000)
    private String token;
}
