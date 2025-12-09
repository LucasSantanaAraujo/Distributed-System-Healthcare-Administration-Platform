package com.psoft.clinic.repository;

import com.psoft.clinic.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends MongoRepository<Patient, Long> {

    List<Patient> findByNameContainingIgnoreCase(String name);

    Optional<Patient> findByUserId(Long userId);
}
