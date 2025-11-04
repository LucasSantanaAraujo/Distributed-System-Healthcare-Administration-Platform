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

@Service
@RequiredArgsConstructor
public class ClientsServiceClient {

    private final RestTemplate restTemplate;

    @Value("${clients.service.internal-key}")
    private String internalApiKey;

    @Value("${clients.service.url}")
    private String clientsServiceUrl;

    public PatientClientResponse getPatientById(Long id) {
        String url = clientsServiceUrl + "/patients/" + id;
        System.out.println("➡️ Teste com RestTemplate: " + url);

        try {
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
                System.out.println("✅ Paciente recebido do clients-service: " + response.getBody());
                return response.getBody();
            } else {
                System.err.println("⚠️ Resposta inesperada: " + response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Erro no RestTemplate: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
