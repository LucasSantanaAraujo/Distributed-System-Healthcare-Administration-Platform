package com.psoft.clinic.saga;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "saga_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaInstance {

    @Id
    private String correlationId;

    private String sagaType;
    private String status;
    private Long patientId;       // ou Long, depende do teu Patient.getId()
    private Boolean eventPublished;

    // Em vez de "_id", usamos "entityId" (mais limpo)
    private Long entityId;

    private String error;

    private Instant createdAt;
    private Instant updatedAt;



}
