package com.psoft.clinic.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "patients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    private Long id; // Agora gerido manualmente no service (não há @GeneratedValue)

    private String userId; // ID do utilizador do auth-service
    private String name;
    private LocalDate birthDate;
    private String contact;
    private String insuranceInfo;
    private Boolean consent;
    private String photoBase64;
    private String healthConcerns;
    private LocalDate lastAppointmentDate;
    @Builder.Default
    private Integer appointmentCount = 0;
}
