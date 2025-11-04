package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId; // ID do utilizador do auth-service

    private String name;
    private LocalDate birthDate;
    private String contact;
    private String insuranceInfo;
    private Boolean consent;
    private String photoBase64; // imagem de perfil codificada em base64
    private String healthConcerns; // observações médicas
}
