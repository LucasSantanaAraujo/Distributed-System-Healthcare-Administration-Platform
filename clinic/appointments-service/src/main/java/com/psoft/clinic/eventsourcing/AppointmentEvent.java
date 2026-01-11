package com.psoft.clinic.eventsourcing;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "appointment_events",
        indexes = {
                @Index(name = "idx_app_events_aggregate", columnList = "aggregateId"),
                @Index(name = "idx_app_events_corr", columnList = "correlationId")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long aggregateId; // appointmentId

    @Column(nullable = false, length = 50)
    private String aggregateType; // "Appointment"

    @Column(nullable = false, length = 80)
    private String eventType; // APPOINTMENT_CREATED, BOOKING_COMPLETED, ...

    @Column(nullable = false, length = 80)
    private String correlationId; // o correlationId da saga

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;


    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private int version;
}
