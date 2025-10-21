package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Physician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String specialty;
    private String contact;
    private String schedule;
    private String photoPath;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
