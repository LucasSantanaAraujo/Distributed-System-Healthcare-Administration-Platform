package com.psoft.clinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.psoft.clinic")
public class ClientsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientsServiceApplication.class, args);
    }
}
