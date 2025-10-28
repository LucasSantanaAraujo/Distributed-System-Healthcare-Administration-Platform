package com.psoft.clinic.client;

import com.psoft.clinic.dto.PatientClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientsServiceClient {

    private final WebClient webClient;

    @Value("${clients.service.url:http://localhost:8082}")
    private String clientsServiceUrl;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public List<PatientClientResponse> getAllPatients() {
        try {
            System.out.println("➡️ [DEBUG] A contactar clients-service...");
            System.out.println("➡️ [DEBUG] URL: " + clientsServiceUrl + "/patients");
            System.out.println("➡️ [DEBUG] WebClient utilizado: " + webClient);
            System.out.println("➡️ [DEBUG] internalApiKey enviada = " + internalApiKey);

            List<PatientClientResponse> patients = webClient.get()
                    .uri(clientsServiceUrl + "/patients")
                    .header("X-INTERNAL-KEY", internalApiKey) // 👈 envia a chave interna
                    .retrieve()
                    .bodyToFlux(PatientClientResponse.class)
                    .collectList()
                    .block();

            System.out.println("✅ [DEBUG] Recebida resposta com " + (patients != null ? patients.size() : 0) + " pacientes");
            return patients;

        } catch (WebClientResponseException e) {
            System.err.println("❌ [ERRO HTTP] ao contactar clients-service: " + e.getStatusCode());
            System.err.println("   Corpo da resposta: " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.err.println("❌ [ERRO GERAL] ao contactar clients-service: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
