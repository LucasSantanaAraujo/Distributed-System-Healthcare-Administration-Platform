package com.psoft.clinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.psoft.clinic.client")
public class AppointmentsServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(AppointmentsServiceApplication.class, args);
	}
}

