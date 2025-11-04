package com.psoft.clinic.repository;

import com.psoft.clinic.model.Physician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface PhysicianRepository extends JpaRepository<Physician, Long> {
    List<Physician> findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(String name, String specialty);
    Optional<Physician> findByUserId(Long userId);
    // ⚙️ Métodos antigos do monólito que dependiam de "user" foram removidos.
    // Se precisares de pesquisar por e-mail, será feito via auth-service mais tarde.

    // Exemplo de método simples que podes manter se quiseres testar:
    // Optional<Physician> findByName(String name);
}
