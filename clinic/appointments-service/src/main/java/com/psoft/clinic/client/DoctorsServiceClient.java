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

@Service
@RequiredArgsConstructor
public class DoctorsServiceClient {

    private final RestTemplate restTemplate;

    @Value("${doctors.service.internal-key}")
    private String internalApiKey;

    @Value("${doctors.service.url}")
    private String doctorsServiceUrl;

    public PhysicianClientResponse getPhysicianById(Long id) {
        String url = doctorsServiceUrl + "/physicians/" + id;
        System.out.println("➡️ Teste com RestTemplate (Doctors): " + url);

        try {
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
                System.out.println("✅ Médico recebido do doctors-service: " + response.getBody());
                return response.getBody();
            } else {
                System.err.println("⚠️ Resposta inesperada: " + response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Erro no RestTemplate (Doctors): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
