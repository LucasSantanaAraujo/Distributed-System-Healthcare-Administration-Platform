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

    // ============================================================
    // 1. CONFIGURAÇÃO DE CLIENTS
    // ============================================================
    public static final String CLIENTS_EXCHANGE = "clients-exchange";
    public static final String PATIENT_CREATED_QUEUE = "patient.created.queue";
    public static final String PATIENT_CREATED_ROUTING_KEY = "patient.created";

    @Bean
    public TopicExchange clientsExchange() {
        return new TopicExchange(CLIENTS_EXCHANGE);
    }

    @Bean
    public Queue patientCreatedQueue() {
        return new Queue(PATIENT_CREATED_QUEUE, true); // true = durável
    }

    @Bean
    public Binding bindingPatientCreated(Queue patientCreatedQueue, TopicExchange clientsExchange) {
        return BindingBuilder
                .bind(patientCreatedQueue)
                .to(clientsExchange)
                .with(PATIENT_CREATED_ROUTING_KEY);
    }

    // ============================================================
    // 2. CONFIGURAÇÃO DE APPOINTMENTS
    // ============================================================
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

    // ============================================================
    // 3. CONFIGURAÇÃO GERAL (Converter JSON)
    // ============================================================
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