package com.psoft.clinic.repository;

import com.psoft.clinic.model.Physician;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicianRepository extends MongoRepository<Physician, Long> {
    List<Physician> findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(String name, String specialty);
    Optional<Physician> findByUserId(Long userId);
}

