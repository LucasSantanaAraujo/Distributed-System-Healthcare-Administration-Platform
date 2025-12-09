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

    // ===============
    // APPOINTMENTS → CLIENTS
    // ===============
    public static final String APPOINTMENTS_EXCHANGE = "appointments.exchange";
    public static final String APPOINTMENTS_CREATED_CLIENTS_QUEUE = "appointments.created.clients.queue";
    public static final String APPOINTMENT_CREATED_ROUTING_KEY = "appointment.created";

    @Bean
    public TopicExchange appointmentsExchange() {
        return new TopicExchange(APPOINTMENTS_EXCHANGE);
    }

    @Bean
    public Queue appointmentsCreatedClientsQueue() {
        return new Queue(APPOINTMENTS_CREATED_CLIENTS_QUEUE, true);
    }

    @Bean
    public Binding appointmentCreatedClientsBinding() {
        return BindingBuilder
                .bind(appointmentsCreatedClientsQueue())
                .to(appointmentsExchange())
                .with(APPOINTMENT_CREATED_ROUTING_KEY);
    }



    // ======================
    // CONFIGURAÇÃO GERAL
    // ======================

    @Bean
    public MessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate tpl = new RabbitTemplate(connectionFactory);
        tpl.setMessageConverter(jsonConverter());
        return tpl;
    }
}
