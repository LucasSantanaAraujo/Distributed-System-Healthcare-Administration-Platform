package com.psoft.clinic.repository;

import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.model.Physician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório de consultas (appointments) pertencente ao doctors-service.
 * Neste serviço, já não existe a entidade Patient localmente.
 * As referências a Patient ficam comentadas até que o clients-service esteja implementado.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStatus(AppointmentStatus status);

    // 🔸 Este método dependia da entidade Patient (do clients-service)
    // List<Appointment> findByPatient(Patient patient);

    List<Appointment> findByPhysician(Physician physician);

    // 🔸 Este método dependia de Patient também — será refeito com REST futuramente
    // List<Appointment> findByPhysicianAndPatientId(Physician physician, Long patientId);

    boolean existsByPhysicianAndDateTime(Physician physician, LocalDateTime dateTime);

    List<Appointment> findByPhysicianAndDateTimeBetween(
            Physician physician,
            LocalDateTime start,
            LocalDateTime end
    );

    // 🔸 Dependia de Patient — será refeito com verificação via clients-service
    // boolean existsByPatientAndDateTime(Patient patient, LocalDateTime dateTime);

    List<Appointment> findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime dateTime);

    List<Appointment> findByPhysicianIdAndDateTime(Long physicianId, LocalDateTime dateTime);

    // Consultas por intervalo de datas
    @Query("SELECT a FROM Appointment a WHERE a.physician = :physician AND a.dateTime >= :start AND a.dateTime < :end")
    List<Appointment> findByPhysicianAndDate(
            @Param("physician") Physician physician,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Médicos com mais consultas
    @Query("SELECT a.physician, COUNT(a) as total FROM Appointment a GROUP BY a.physician ORDER BY total DESC")
    List<Object[]> findTopPhysiciansByAppointmentCount(Pageable pageable);

    // Total de consultas por mês/ano
    @Query("SELECT COUNT(a) FROM Appointment a WHERE MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // Canceladas
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = 'CANCELLED' AND MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countCancelledByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // Reagendadas
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = 'RESCHEDULED' AND MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countRescheduledByMonthAndYear(@Param("month") int month, @Param("year") int year);
}
