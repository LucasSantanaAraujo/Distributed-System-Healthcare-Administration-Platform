package com.psoft.clinic.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "appointment_records")
public class AppointmentRecord {

    @Id
    private String id; // Agora é String (ObjectId do Mongo)

    private Long appointmentId; // referência à consulta (do appointments-service)
    private String diagnosis;
    private String treatment;
    private String prescription;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}
