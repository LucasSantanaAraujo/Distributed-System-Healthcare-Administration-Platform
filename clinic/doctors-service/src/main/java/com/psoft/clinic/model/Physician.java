package com.psoft.clinic.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "physicians")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Physician {

    @Id
    private Long id; // <-- agora é Long, igual ao clients-service

    private Long userId;

    private String name;
    private String specialty;
    private String contact;
    private String schedule;
    private String photoPath;
    private Integer totalAppointments = 0;
    private LocalDateTime lastAppointment;
}

