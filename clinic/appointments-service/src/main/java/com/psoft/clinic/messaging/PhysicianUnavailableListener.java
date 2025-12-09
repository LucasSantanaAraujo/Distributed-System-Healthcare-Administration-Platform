package com.psoft.clinic.messaging;

import com.psoft.clinic.events.PhysicianUnavailableEvent;
import com.psoft.clinic.model.Appointment;
import com.psoft.clinic.model.AppointmentStatus;
import com.psoft.clinic.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PhysicianUnavailableListener {

    private final AppointmentRepository appointmentRepository;

    @RabbitListener(queues = "physician.unavailable.queue")
    public void onPhysicianUnavailable(PhysicianUnavailableEvent event) {

        LocalDate from = LocalDate.parse(event.getFrom());
        LocalDate to = LocalDate.parse(event.getTo());

        var appts = appointmentRepository
                .findByPhysicianIdAndDateTimeBetween(event.getPhysicianId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        for (Appointment a : appts) {
            a.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(a);
        }

        System.out.println("❌ Consultas canceladas para médico " + event.getPhysicianId());
    }
}
