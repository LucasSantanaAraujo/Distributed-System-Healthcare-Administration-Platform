package com.psoft.clinic.eventsourcing;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatientEventRepository extends MongoRepository<PatientEvent, String> {
    Optional<PatientEvent> findTopByAggregateIdOrderByVersionDesc(Long aggregateId);
}