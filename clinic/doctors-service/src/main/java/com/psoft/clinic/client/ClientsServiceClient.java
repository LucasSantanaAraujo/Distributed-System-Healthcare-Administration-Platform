package com.psoft.clinic.client;

import com.psoft.clinic.dto.PatientClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class ClientsServiceClient {

    private final WebClient webClient;
    @Value("${clients.service.internal-key}")
    private String internalApiKey;

    @Value("${clients.service.url:http://localhost:8082}")
    private String clientsServiceUrl;

    public PatientClientResponse getPatientById(Long id) {
        try {
            return webClient.get()
                    .uri(clientsServiceUrl + "/patients/" + id)
                    .header("X-INTERNAL-KEY", internalApiKey) // 👈 envia a chave secreta
                    .retrieve()
                    .bodyToMono(PatientClientResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.err.println("Erro ao contactar clients-service: " + e.getStatusCode());
            return null;
        } catch (Exception e) {
            System.err.println("Erro genérico ao contactar clients-service: " + e.getMessage());
            return null;
        }
    }

}
