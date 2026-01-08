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
    private String status;        // STARTED, BOOKED, FAILED
    private Long appointmentId;   // preenchido quando tiver sucesso
    private String error;         // preenchido se falhar

    private Instant createdAt;
    private Instant updatedAt;
}
