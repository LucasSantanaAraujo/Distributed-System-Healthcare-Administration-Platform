package com.psoft.clinic.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psoft.clinic.dto.PatientClientResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
@RequiredArgsConstructor
public class ClientsServiceClient {

    private final RestTemplate restTemplate;

    @Value("${clients.service.internal-key}")
    private String internalApiKey;

    @Value("${clients.service.url}")
    private String clientsServiceUrl;

    @CircuitBreaker(name = "clientsService", fallbackMethod = "getPatientByIdFallback")
    @Retry(name = "clientsService")
    public PatientClientResponse getPatientById(Long id) {
        String url = clientsServiceUrl + "/patients/" + id;
        System.out.println("➡️ [clients-service] GET " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-INTERNAL-KEY", internalApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PatientClientResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                PatientClientResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        return null;
    }

    // fallback tem de ter a mesma assinatura + Throwable no fim
    private PatientClientResponse getPatientByIdFallback(Long id, Throwable t) {
        System.err.println("⚠️ [clients-service] fallback (CB/Retry): " + t.getClass().getSimpleName() + " - " + t.getMessage());
        return null;
    }

}
