package com.psoft.clinic.eventsourcing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppointmentEventStore {

    private final AppointmentEventRepository repo;
    private final ObjectMapper objectMapper;

    public AppointmentEvent append(Long appointmentId,
                                   String eventType,
                                   String correlationId,
                                   Object payloadObj) {

        try {
            int nextVersion = repo.findTopByAggregateIdOrderByVersionDesc(appointmentId)
                    .map(e -> e.getVersion() + 1)
                    .orElse(1);

            Map<String, Object> payloadMap = objectMapper.convertValue(
                    payloadObj,
                    new TypeReference<Map<String, Object>>() {}
            );

            AppointmentEvent saved = repo.save(AppointmentEvent.builder()
                    .aggregateId(appointmentId)
                    .aggregateType("Appointment")
                    .eventType(eventType)
                    .correlationId(correlationId)
                    .payload(payloadMap)
                    .occurredAt(Instant.now())
                    .version(nextVersion)
                    .build());

            return saved;

        } catch (Exception e) {
            throw new RuntimeException("Falha ao gravar evento no Event Store: " + e.getMessage(), e);
        }
    }
}
