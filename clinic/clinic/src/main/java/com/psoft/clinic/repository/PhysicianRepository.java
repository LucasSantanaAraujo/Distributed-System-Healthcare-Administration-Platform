package com.psoft.clinic.repository;

import com.psoft.clinic.model.Physician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhysicianRepository extends JpaRepository<Physician, Long> {
    List<Physician> findByNameContainingIgnoreCase(String name);
    List<Physician> findBySpecialtyContainingIgnoreCase(String specialty);
    List<Physician> findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(String name, String specialty);
    Optional<Physician> findByUserEmail(String email);

}
