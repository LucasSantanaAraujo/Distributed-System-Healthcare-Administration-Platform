package com.psoft.clinic.repository;

import com.psoft.clinic.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByNameContainingIgnoreCase(String name);
    Optional<Patient> findByUserId(Long userId);

}
