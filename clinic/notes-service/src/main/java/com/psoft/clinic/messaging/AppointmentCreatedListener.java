package com.psoft.clinic.messaging;

import com.psoft.clinic.events.AppointmentCreatedEvent;
import com.psoft.clinic.model.AppointmentRecord;
import com.psoft.clinic.repository.AppointmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentCreatedListener {

    private final AppointmentRecordRepository repository;

    @RabbitListener(queues = "appointments.created.queue")
    public void onAppointmentCreated(AppointmentCreatedEvent event) {

        System.out.println("📩 [NOTES] Recebido AppointmentCreatedEvent: " + event);

        AppointmentRecord record = AppointmentRecord.builder()
                .appointmentId(event.getAppointmentId())
                .diagnosis("Ainda sem diagnóstico")
                .treatment("Ainda sem tratamento")
                .prescription("Nenhuma prescrição")
                .build();

        repository.save(record);

        System.out.println("📝 [NOTES] Criado AppointmentRecord automático para ID = " + event.getAppointmentId());
    }
}
