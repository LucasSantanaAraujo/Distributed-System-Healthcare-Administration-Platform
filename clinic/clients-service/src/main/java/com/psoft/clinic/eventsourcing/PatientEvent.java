package com.psoft.clinic.eventsourcing;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "patient_events")
@CompoundIndexes({
        @CompoundIndex(name = "agg_ver_idx", def = "{'aggregateId': 1, 'version': 1}", unique = true)
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEvent {

    @Id
    private String id;

    private Long aggregateId; // Patient ID
    private String aggregateType; // "Patient"
    private String eventType; // PATIENT_CREATED, PATIENT_UPDATED
    private String correlationId; // Saga ID

    private Map<String, Object> payload; // Dados do paciente

    private Instant occurredAt;
    private int version;
}