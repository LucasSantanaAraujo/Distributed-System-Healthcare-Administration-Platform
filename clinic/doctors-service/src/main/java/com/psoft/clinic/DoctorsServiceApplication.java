package com.psoft.clinic;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import 	org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@EnableRabbit

@SpringBootApplication
public class DoctorsServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(DoctorsServiceApplication.class, args);
	}
}

