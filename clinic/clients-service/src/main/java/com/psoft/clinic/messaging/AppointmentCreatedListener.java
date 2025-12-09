package com.psoft.clinic.messaging;

import com.psoft.clinic.events.AppointmentCreatedEvent;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AppointmentCreatedListener {

    private final PatientRepository patientRepository;

    @RabbitListener(queues = "appointments.created.clients.queue")
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {

        System.out.println("📩 [CLIENTS] AppointmentCreatedEvent recebido: " + event);

        Patient p = patientRepository.findById(event.getPatientId()).orElse(null);
        if (p == null) {
            System.out.println("⚠️ [CLIENTS] Paciente não encontrado para ID = " + event.getPatientId());
            return;
        }

        // Atualiza última consulta e contador
        LocalDate date = event.getDateTime() != null
                ? event.getDateTime().toLocalDate()
                : LocalDate.now();

        p.setLastAppointmentDate(date);

        Integer currentCount = p.getAppointmentCount();
        if (currentCount == null) currentCount = 0;
        p.setAppointmentCount(currentCount + 1);

        patientRepository.save(p);

        System.out.println("📌 [CLIENTS] Cliente atualizado após consulta: " + p.getName()
                + " | total consultas = " + p.getAppointmentCount());
    }
}
