package com.psoft.clinic.repository;

import com.psoft.clinic.model.AppointmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, Long> {
}
