package com.psoft.clinic.client;

// Imports do Resilience4j
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

// Imports do Spring
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IdentityServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    // Deve coincidir com a configuração no application.yml
    private static final String SERVICE_NAME = "identityService";

    @CircuitBreaker(name = SERVICE_NAME, fallbackMethod = "validateUserFallback")
    public boolean isValidUser(Long userId) {
        // Exemplo: http://localhost:8081/api/users/{id}
        // Nota: Garante que tens o URL correto do serviço de identidade/auth
        String url = "http://localhost:8081/api/internal/users/" + userId + "/exists";

        try {
            return Boolean.TRUE.equals(restTemplate.getForObject(url, Boolean.class));
        } catch (Exception e) {
            throw e; // Lança a exceção para ativar o Circuit Breaker se for erro de rede
        }
    }

    // A assinatura do método fallback DEVE aceitar os mesmos argumentos + Throwable
    public boolean validateUserFallback(Long userId, Throwable t) {
        System.err.println("⚠️ Circuit Breaker aberto ou Timeout no IdentityService: " + t.getMessage());
        // Lógica de Falha:
        // return false; // Impede a criação se não conseguir validar
        // OU
        return true; // (Modo degradado) Assume válido temporariamente se o serviço cair
    }
}