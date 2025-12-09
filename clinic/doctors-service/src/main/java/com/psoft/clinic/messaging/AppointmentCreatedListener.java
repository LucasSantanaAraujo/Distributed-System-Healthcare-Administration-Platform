package com.psoft.clinic.messaging;

import com.psoft.clinic.events.AppointmentCreatedEvent;
import com.psoft.clinic.model.Physician;
import com.psoft.clinic.repository.PhysicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentCreatedListener {

    private final PhysicianRepository physicianRepository;

    @RabbitListener(queues = "appointments.created.doctors.queue")
    public void onAppointmentCreated(AppointmentCreatedEvent event) {

        System.out.println("📩 [DOCTORS] Evento recebido: " + event);

        Physician doctor = physicianRepository.findByUserId(event.getPhysicianId()).orElse(null);
        if (doctor == null) {
            System.out.println("⚠️ [DOCTORS] Médico não encontrado para ID = " + event.getPhysicianId());
            return;
        }

        // Atualizar estatísticas
        doctor.setTotalAppointments(
                doctor.getTotalAppointments() == null ? 1 : doctor.getTotalAppointments() + 1
        );

        doctor.setLastAppointment(event.getDateTime());

        physicianRepository.save(doctor);

        System.out.println("👨‍⚕️ [DOCTORS] Estatísticas do médico atualizadas: " + doctor.getName());
    }
}
