package com.psoft.clinic.eventsourcing;

import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppointmentProjector {

    private final AppointmentRepository appointmentRepository;

    @Transactional
    public void project(AppointmentEvent event) {
        String type = event.getEventType();
        Map<String, Object> p = event.getPayload();

        switch (type) {
            case "APPOINTMENT_CREATED" -> {
                Long id = event.getAggregateId();

                Appointment a = appointmentRepository.findById(id)
                        .orElse(Appointment.builder().id(id).build());

                a.setPatientId(asLong(p.get("patientId")));
                a.setPhysicianId(asLong(p.get("physicianId")));
                a.setDateTime(LocalDateTime.parse((String) p.get("dateTime")));
                a.setReason((String) p.get("reason"));
                a.setStatus(AppointmentStatus.valueOf((String) p.get("status")));

                appointmentRepository.save(a);
            }

            case "APPOINTMENT_STATUS_CHANGED" -> {
                Long id = event.getAggregateId();
                var opt = appointmentRepository.findById(id);
                if (opt.isEmpty()) return;

                Appointment a = opt.get();
                a.setStatus(AppointmentStatus.valueOf((String) p.get("status")));
                appointmentRepository.save(a);
            }

            case "APPOINTMENT_CANCELLED" -> {
                Long id = event.getAggregateId();
                var opt = appointmentRepository.findById(id);
                if (opt.isEmpty()) return;

                Appointment a = opt.get();
                a.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(a);
            }

            default -> {
                // eventos que não afetam a projeção podem ser ignorados
            }
        }
    }

    private Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }
}
