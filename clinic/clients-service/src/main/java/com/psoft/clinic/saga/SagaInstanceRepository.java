package com.psoft.clinic.saga;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SagaInstanceRepository extends MongoRepository<SagaInstance, String> {
    Optional<SagaInstance> findByCorrelationId(String correlationId);
}
