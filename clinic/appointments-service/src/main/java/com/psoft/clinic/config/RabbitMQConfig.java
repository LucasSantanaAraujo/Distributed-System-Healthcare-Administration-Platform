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

    public static final String EXCHANGE = "appointments.exchange";
    public static final String CREATED_QUEUE = "appointments.created.queue";
    public static final String CREATED_ROUTING_KEY = "appointment.created";

    @Bean
    public TopicExchange appointmentsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue appointmentCreatedQueue() {
        return new Queue(CREATED_QUEUE, true);
    }

    @Bean
    public Binding createdBinding() {
        return BindingBuilder.bind(appointmentCreatedQueue())
                .to(appointmentsExchange())
                .with(CREATED_ROUTING_KEY);
    }

    // 🔥 Converter JSON → necessário para enviar objetos
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 🔥 RabbitTemplate com o conversor JSON
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
