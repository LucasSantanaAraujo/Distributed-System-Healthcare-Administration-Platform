package com.psoft.clinic.client;

import com.psoft.clinic.dto.PhysicianClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;


@Service
@RequiredArgsConstructor
public class DoctorsServiceClient {

    private final RestTemplate restTemplate;

    @Value("${doctors.service.internal-key}")
    private String internalApiKey;

    @Value("${doctors.service.url}")
    private String doctorsServiceUrl;

    @CircuitBreaker(name = "doctorsService", fallbackMethod = "getPhysicianByIdFallback")
    @Retry(name = "doctorsService")
    public PhysicianClientResponse getPhysicianById(Long id) {
        String url = doctorsServiceUrl + "/physicians/" + id;
        System.out.println("➡️ [doctors-service] GET " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-INTERNAL-KEY", internalApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PhysicianClientResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                PhysicianClientResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        return null;
    }

    private PhysicianClientResponse getPhysicianByIdFallback(Long id, Throwable t) {
        System.err.println("⚠️ [doctors-service] fallback (CB/Retry): "
                + t.getClass().getSimpleName() + " - " + t.getMessage());
        return null;
    }

}
