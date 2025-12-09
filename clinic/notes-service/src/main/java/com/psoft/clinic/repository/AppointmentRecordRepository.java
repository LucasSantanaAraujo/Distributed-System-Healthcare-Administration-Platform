package com.psoft.clinic.repository;

import com.psoft.clinic.model.AppointmentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AppointmentRecordRepository extends MongoRepository<AppointmentRecord, String> {

    Optional<AppointmentRecord> findByAppointmentId(Long appointmentId);
}
