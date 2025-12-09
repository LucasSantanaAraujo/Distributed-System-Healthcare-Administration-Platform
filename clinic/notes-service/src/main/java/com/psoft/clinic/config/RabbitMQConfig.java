package com.psoft.clinic.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String APPOINTMENTS_EXCHANGE = "appointments.exchange";
    public static final String APPOINTMENT_CREATED_QUEUE = "notes.appointment.created.queue";
    public static final String APPOINTMENT_CREATED_ROUTING_KEY = "appointment.created";

    // Exchange que o appointments-service usa
    @Bean
    public TopicExchange appointmentsExchange() {
        return new TopicExchange(APPOINTMENTS_EXCHANGE);
    }

    // Queue específica para o notes-service
    @Bean
    public Queue appointmentCreatedQueue() {
        return new Queue(APPOINTMENT_CREATED_QUEUE, true);
    }

    // Ligação entre queue e exchange
    @Bean
    public Binding appointmentCreatedBinding() {
        return BindingBuilder
                .bind(appointmentCreatedQueue())
                .to(appointmentsExchange())
                .with(APPOINTMENT_CREATED_ROUTING_KEY);
    }




    // Converter JSON
    @Bean
    public MessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate com JSON
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonConverter());
        return template;
    }
}
