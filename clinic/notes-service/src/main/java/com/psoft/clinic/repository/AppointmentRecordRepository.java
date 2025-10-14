package com.psoft.clinic.repository;

import com.psoft.clinic.model.AppointmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, Long> {
    Optional<AppointmentRecord> findByAppointmentId(Long appointmentId);
}
