package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String token;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    // 🔹 Ligação com Patient (mantida, pertence a este serviço)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Patient patient;

    // 🔸 Ligação com Physician removida (pertence ao doctors-service)
    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    // private Physician physician;
}
