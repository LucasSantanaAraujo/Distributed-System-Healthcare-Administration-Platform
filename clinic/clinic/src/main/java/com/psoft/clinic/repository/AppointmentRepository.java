package com.psoft.clinic.repository;

import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.model.Physician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByPatient(Patient patient);
    List<Appointment> findByPhysician(Physician physician);
    List<Appointment> findByPhysicianAndPatientId(Physician physician, Long patientId);
    boolean existsByPhysicianAndDateTime(Physician physician, LocalDateTime dateTime);
    List<Appointment> findByPhysicianAndDateTimeBetween(Physician physician, LocalDateTime start, LocalDateTime end);
    boolean existsByPatientAndDateTime(Patient patient, LocalDateTime dateTime);
    List<Appointment> findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime dateTime);
    List<Appointment> findByPhysicianIdAndDateTime(Long physicianId, LocalDateTime dateTime);

    // AppointmentRepository.java
    @Query("SELECT a FROM Appointment a WHERE a.physician = :physician AND a.dateTime >= :start AND a.dateTime < :end")
    List<Appointment> findByPhysicianAndDate(
            @Param("physician") Physician physician,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    // AppointmentRepository.java
    @Query("SELECT a.physician, COUNT(a) as total FROM Appointment a GROUP BY a.physician ORDER BY total DESC")
    List<Object[]> findTopPhysiciansByAppointmentCount(Pageable pageable);

    // Total appointments for a given month/year
    @Query("SELECT COUNT(a) FROM Appointment a WHERE MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = 'CANCELLED' AND MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countCancelledByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // Se tiver status "RESCHEDULED", faz igual:
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = 'RESCHEDULED' AND MONTH(a.dateTime) = :month AND YEAR(a.dateTime) = :year")
    Long countRescheduledByMonthAndYear(@Param("month") int month, @Param("year") int year);










}
