package com.psoft.clinic.client;

import com.psoft.clinic.dto.PhysicianClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class DoctorsServiceClient {

    private final RestTemplate restTemplate;

    @Value("${doctors.service.url:http://localhost:8082}")
    private String doctorsServiceUrl;

    public PhysicianClientResponse getPhysicianById(Long physicianId) {
        try {
            return restTemplate.getForObject(
                    doctorsServiceUrl + "/physicians/" + physicianId,
                    PhysicianClientResponse.class
            );
        } catch (Exception e) {
            System.err.println("Erro ao contactar o doctors-service: " + e.getMessage());
            return null;
        }
    }
}
