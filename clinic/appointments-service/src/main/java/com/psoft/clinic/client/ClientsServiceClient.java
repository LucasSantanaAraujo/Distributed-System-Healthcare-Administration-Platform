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
import io.github.resilience4j.bulkhead.annotation.Bulkhead;


@Service
@RequiredArgsConstructor
public class ClientsServiceClient {

    private final RestTemplate restTemplate;

    @Value("${clients.service.internal-key}")
    private String internalApiKey;

    @Value("${clients.service.url}")
    private String clientsServiceUrl;

    @Bulkhead(name = "clientsService")
    @CircuitBreaker(name = "clientsService", fallbackMethod = "getPatientByIdFallback")
    @Retry(name = "clientsService")
    public RemoteCallResult<PatientClientResponse> getPatientById(Long id) {
        String url = clientsServiceUrl + "/patients/" + id;
        System.out.println("➡️ [clients-service] GET " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-INTERNAL-KEY", internalApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PatientClientResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, PatientClientResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return RemoteCallResult.ok(response.getBody());
        }

        if (response.getStatusCode().value() == 404) {
            return RemoteCallResult.notFound("Paciente não encontrado.");
        }

        if (response.getStatusCode().value() == 403) {
            return RemoteCallResult.forbidden("Acesso negado ao clients-service.");
        }

        if (response.getStatusCode().is4xxClientError()) {
            return RemoteCallResult.badRequest("Erro 4xx do clients-service: " + response.getStatusCode());
        }

        return RemoteCallResult.unavailable("Erro do clients-service: " + response.getStatusCode());
    }

    private RemoteCallResult<PatientClientResponse> getPatientByIdFallback(Long id, Throwable t) {
        System.err.println("⚠️ [clients-service] fallback (CB/Retry/Bulkhead): "
                + t.getClass().getSimpleName() + " - " + t.getMessage());
        return RemoteCallResult.unavailable("clients-service indisponível: " + t.getClass().getSimpleName());
    }


}
