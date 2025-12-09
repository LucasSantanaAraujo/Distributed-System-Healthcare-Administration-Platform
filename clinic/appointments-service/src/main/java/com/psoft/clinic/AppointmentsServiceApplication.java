package com.psoft.clinic;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@EnableRabbit
@SpringBootApplication
@EnableFeignClients(basePackages = "com.psoft.clinic.client")
public class AppointmentsServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(AppointmentsServiceApplication.class, args);
	}
}

