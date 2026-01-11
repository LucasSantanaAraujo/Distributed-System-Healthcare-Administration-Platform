package com.psoft.clinic.saga;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaInstanceRepository extends MongoRepository<SagaInstance, String> {
}
