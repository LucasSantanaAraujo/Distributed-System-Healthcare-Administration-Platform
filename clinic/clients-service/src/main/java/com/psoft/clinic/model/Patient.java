package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate birthDate;

    private String contact;

    private String insuranceInfo;

    private boolean consent;

    @Lob
    private String photoBase64;

    @ElementCollection
    private List<String> healthConcerns;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
