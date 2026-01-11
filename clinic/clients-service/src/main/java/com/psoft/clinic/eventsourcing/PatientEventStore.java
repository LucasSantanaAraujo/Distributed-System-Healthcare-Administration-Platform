package com.psoft.clinic.eventsourcing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientEventStore {

    private final PatientEventRepository repo;
    private final ObjectMapper objectMapper;

    public PatientEvent append(Long patientId, String eventType, String correlationId, Object payloadObj) {
        int nextVersion = repo.findTopByAggregateIdOrderByVersionDesc(patientId)
                .map(e -> e.getVersion() + 1)
                .orElse(1);

        Map<String, Object> payloadMap = objectMapper.convertValue(payloadObj, Map.class);

        PatientEvent event = PatientEvent.builder()
                .aggregateId(patientId)
                .aggregateType("Patient")
                .eventType(eventType)
                .correlationId(correlationId)
                .payload(payloadMap)
                .occurredAt(Instant.now())
                .version(nextVersion)
                .build();

        return repo.save(event);
    }
}