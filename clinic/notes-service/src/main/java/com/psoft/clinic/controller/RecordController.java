package com.psoft.clinic.controller;

import com.psoft.clinic.dto.RecordRequest;
import com.psoft.clinic.model.AppointmentRecord;
import com.psoft.clinic.repository.AppointmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final AppointmentRecordRepository repository;

    @PostMapping
    public ResponseEntity<?> createRecord(@RequestBody RecordRequest request) {
        AppointmentRecord record = AppointmentRecord.builder()
                .appointmentId(request.getAppointmentId())
                .diagnosis(request.getDiagnosis())
                .treatment(request.getTreatment())
                .prescription(request.getPrescription())
                .build();
        repository.save(record);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<?> getRecordByAppointment(@PathVariable Long appointmentId) {
        return repository.findByAppointmentId(appointmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
