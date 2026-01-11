package com.psoft.clinic.saga;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "saga_instances")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaInstance {

    @Id
    private String correlationId;

    private String sagaType;      // "BOOK_APPOINTMENT"
    private String status;        // STARTED, BOOKED, FAILED, FAILED_COMPENSATED...
    private String currentStep;   // VALIDATE_PATIENT, CREATE_APPOINTMENT, PUBLISH_EVENT...
    private Long appointmentId;
    private String error;

    private Instant createdAt;
    private Instant updatedAt;
}
