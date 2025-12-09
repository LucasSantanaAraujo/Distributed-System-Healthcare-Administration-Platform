package com.psoft.clinic.repository;

import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório de consultas (appointments) pertencente ao appointments-service.
 * Este serviço apenas armazena IDs de paciente e médico, não entidades completas.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStatus(AppointmentStatus status);

    // Consultas por ID do médico
    List<Appointment> findByPhysicianId(Long physicianId);

  ;
    boolean existsByPatientIdAndDateTime(Long patientId, LocalDateTime dateTime);

    List<Appointment> findByPhysicianIdAndDateTime(Long physicianId, LocalDateTime dateTime);

    // Verificar se existe conflito de horário para um médico específico
    boolean existsByPhysicianIdAndDateTime(Long physicianId, LocalDateTime dateTime);

    // Consultas de um médico num determinado intervalo de tempo
    List<Appointment> findByPhysicianIdAndDateTimeBetween(
            Long physicianId,
            LocalDateTime start,
            LocalDateTime end
    );
    

    // Consultas futuras (ordenadas)
    List<Appointment> findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime dateTime);

    // 🔹 Estatísticas e relatórios (mantidos com base em IDs)
    @Query("SELECT a.physicianId, COUNT(a) as total FROM Appointment a GROUP BY a.physicianId ORDER BY total DESC")
    List<Object[]> findTopPhysiciansByAppointmentCount(Pageable pageable);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = 'CANCELLED' AND MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countCancelledByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = 'RESCHEDULED' AND MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countRescheduledByMonthAndYear(@Param("month") int month, @Param("year") int year);


}
