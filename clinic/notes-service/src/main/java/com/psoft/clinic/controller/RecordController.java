package com.psoft.clinic.controller;

import com.psoft.clinic.client.AppointmentsServiceClient;
import com.psoft.clinic.dto.AppointmentBasicResponse;
import com.psoft.clinic.dto.AppointmentDetailsRequest;
import com.psoft.clinic.dto.AppointmentResponse;
import com.psoft.clinic.model.AppointmentRecord;
import com.psoft.clinic.repository.AppointmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecordController {

    private static final Logger logger = LoggerFactory.getLogger(RecordController.class);
    private final AppointmentRecordRepository recordRepository;
    private final AppointmentsServiceClient appointmentsServiceClient;

    // ============================================================
    // MÉDICO → cria/atualiza detalhes de uma consulta (por appointmentId)
    // ============================================================
    @PreAuthorize("hasRole('PHYSICIAN')")
    @PutMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> upsertByAppointment(@PathVariable Long appointmentId,
                                                 @RequestBody AppointmentDetailsRequest request) {
        try {
            logger.info("🩺 [upsertByAppointment] Atualizar registo para consulta {}", appointmentId);

            // 🔍 Verifica se a consulta existe no appointments-service
            AppointmentBasicResponse appointment = appointmentsServiceClient.getAppointment(appointmentId);
            if (appointment == null) {
                logger.warn("⚠️ [upsertByAppointment] Consulta {} não encontrada no appointments-service", appointmentId);
                return ResponseEntity.badRequest().body("Consulta inexistente no appointments-service.");
            }

            // 🔧 Cria ou atualiza o registo
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
            rec.setUpdatedAt(LocalDateTime.now());

            recordRepository.save(rec);

            logger.info("✅ [upsertByAppointment] Registo atualizado para consulta {}", appointmentId);
            return ResponseEntity.ok("Detalhes atualizados com sucesso.");

        } catch (Exception e) {
            logger.error("❌ [upsertByAppointment] Erro ao atualizar registo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erro ao atualizar registo: " + e.getMessage());
        }
    }

    // ============================================================
    // ADMIN / PACIENTE → obter registo por ID
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','PATIENT')")
    @GetMapping("/{recordId}")
    public ResponseEntity<?> getById(@PathVariable Long recordId) {
        logger.info("📄 [getById] Pedido de registo ID {}", recordId);

        Optional<AppointmentRecord> opt = recordRepository.findByAppointmentId(recordId);
        if (opt.isEmpty()) {
            logger.warn("⚠️ [getById] Registo {} não encontrado", recordId);
            return ResponseEntity.notFound().build();
        }

        AppointmentRecord r = opt.get();

        AppointmentResponse resp = AppointmentResponse.builder()
                .id(r.getAppointmentId())
                .dateTime(null) // podes enriquecer com dados do appointments-service se quiseres
                .reason(null)
                .status(null)
                .patientName(null)
                .physicianName(null)
                .diagnosis(r.getDiagnosis())
                .treatment(r.getTreatment())
                .prescription(r.getPrescription())
                .build();

        logger.info("📤 [getById] Registo devolvido com sucesso");
        return ResponseEntity.ok(resp);
    }
}
