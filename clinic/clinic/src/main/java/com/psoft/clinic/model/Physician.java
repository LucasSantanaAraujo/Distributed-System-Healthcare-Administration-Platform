package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "physicians")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Physician {
    private String photoPath;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    private String name;

    private String specialty;

    private String contact;

    private String schedule;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
