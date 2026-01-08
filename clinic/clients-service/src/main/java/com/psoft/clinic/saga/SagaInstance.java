package com.psoft.clinic.saga;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "saga_instances") // Mudou de @Entity/@Table para @Document
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaInstance {

    @Id // Importado de org.springframework.data.annotation.Id
    private String correlationId;

    private String sagaType;
    private String status;
    private Long appointmentId;   // (Aqui representa o patientId)
    private String error;

    private Instant createdAt;
    private Instant updatedAt;
}