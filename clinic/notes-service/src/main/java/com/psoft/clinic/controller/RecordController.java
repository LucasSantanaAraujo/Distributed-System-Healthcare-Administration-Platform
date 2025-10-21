package com.psoft.clinic.controller;

import com.psoft.clinic.dto.AppointmentDetailsRequest;
import com.psoft.clinic.dto.AppointmentResponse;
import com.psoft.clinic.model.AppointmentRecord;
import com.psoft.clinic.repository.AppointmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final AppointmentRecordRepository recordRepository;
    // Opcional: private final AppointmentsServiceClient appointmentsServiceClient;

    // PHYSICIAN → cria/atualiza detalhes de uma consulta (por appointmentId)
    @PreAuthorize("hasRole('PHYSICIAN')")
    @PutMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> upsertByAppointment(@PathVariable Long appointmentId,
                                                 @RequestBody AppointmentDetailsRequest request) {

        Optional<AppointmentRecord> existing = recordRepository.findByAppointmentId(appointmentId);

        AppointmentRecord rec = existing.orElseGet(() ->
                AppointmentRecord.builder()
                        .appointmentId(appointmentId)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        rec.setDiagnosis(request.getDiagnosis());
        rec.setTreatment(request.getTreatment());
        rec.setPrescription(request.getPrescription());

        recordRepository.save(rec);
        return ResponseEntity.ok("Detalhes atualizados com sucesso.");
    }

    // ADMIN/PATIENT → obter registo por id
    @PreAuthorize("hasAnyRole('ADMIN','PATIENT')")
    @GetMapping("/{recordId}")
    public ResponseEntity<?> getById(@PathVariable Long recordId) {
        Optional<AppointmentRecord> opt = recordRepository.findById(recordId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        AppointmentRecord r = opt.get();

        // Best-effort de resposta compatível com o teu DTO antigo
        AppointmentResponse resp = AppointmentResponse.builder()
                .id(r.getAppointmentId())
                .dateTime(null) // sem data local; poderia vir do appointments-service
                .reason(null)
                .status(null)
                .patientName(null)    // poderia ser enriquecido com clients-service
                .physicianName(null)  // e doctors-service
                .diagnosis(r.getDiagnosis())
                .treatment(r.getTreatment())
                .prescription(r.getPrescription())
                .build();

        return ResponseEntity.ok(resp);
    }
}
