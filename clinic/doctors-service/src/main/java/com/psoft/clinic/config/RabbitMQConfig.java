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

    public static final String DOCTORS_QUEUE = "appointments.created.doctors.queue";
    public static final String ROUTING_KEY = "appointment.created";

    @Bean
    public TopicExchange appointmentsExchange() {
        return new TopicExchange(APPOINTMENTS_EXCHANGE);
    }

    @Bean
    public Queue doctorsQueue() {
        return new Queue(DOCTORS_QUEUE, true);
    }

    @Bean
    public Binding doctorsBinding() {
        return BindingBuilder
                .bind(doctorsQueue())
                .to(appointmentsExchange())
                .with(ROUTING_KEY);
    }

    public static final String PHYSICIAN_UNAVAILABLE_QUEUE = "physician.unavailable.queue";
    public static final String PHYSICIAN_UNAVAILABLE_ROUTING_KEY = "physician.unavailable";
    public static final String DOCTORS_EXCHANGE = "doctors.exchange";

    @Bean
    public TopicExchange doctorsExchange() {
        return new TopicExchange(DOCTORS_EXCHANGE);
    }

    @Bean
    public Queue physicianUnavailableQueue() {
        return new Queue(PHYSICIAN_UNAVAILABLE_QUEUE, true);
    }

    @Bean
    public Binding physicianUnavailableBinding() {
        return BindingBuilder
                .bind(physicianUnavailableQueue())
                .to(doctorsExchange())
                .with(PHYSICIAN_UNAVAILABLE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate template(ConnectionFactory connectionFactory) {
        RabbitTemplate tpl = new RabbitTemplate(connectionFactory);
        tpl.setMessageConverter(messageConverter());
        return tpl;
    }
}
