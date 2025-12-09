package com.psoft.clinic.messaging;

import com.psoft.clinic.events.AppointmentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCreatedEvent(AppointmentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                "appointments.exchange",
                "appointment.created",
                event
        );

        System.out.println("📢 [RabbitMQ] AppointmentCreatedEvent enviado: " + event);
    }
}
