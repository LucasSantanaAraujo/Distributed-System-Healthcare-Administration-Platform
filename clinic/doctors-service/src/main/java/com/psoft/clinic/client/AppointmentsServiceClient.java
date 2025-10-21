package com.psoft.clinic.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class AppointmentsServiceClient {

    private final WebClient webClient;

    @Value("${appointments.service.url:http://localhost:8083}")
    private String appointmentsServiceUrl;

    @Value("${appointments.service.internal-key:abakaxi}")
    private String internalApiKey;

    /**
     * Obtém todas as consultas associadas a um médico específico.
     */
    public String getAppointmentsByPhysicianId(Long physicianId) {
        try {
            return webClient.get()
                    .uri(appointmentsServiceUrl + "/appointments/physician/" + physicianId)
                    .header("X-INTERNAL-KEY", internalApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.err.println("Erro no appointments-service: " + e.getStatusCode());
            return null;
        } catch (Exception e) {
            System.err.println("Erro ao comunicar com appointments-service: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cria uma nova consulta (appointment) no appointments-service.
     */
    public String createAppointment(String appointmentJson) {
        try {
            return webClient.post()
                    .uri(appointmentsServiceUrl + "/appointments")
                    .header("Content-Type", "application/json")
                    .header("X-INTERNAL-KEY", internalApiKey)
                    .bodyValue(appointmentJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.err.println("Erro no appointments-service: " + e.getStatusCode());
            return null;
        } catch (Exception e) {
            System.err.println("Erro ao criar consulta: " + e.getMessage());
            return null;
        }
    }
    /**
     * Obtém os médicos com mais consultas (TOP 5).
     */
    public String getTopPhysiciansByAppointmentCount() {
        try {
            return webClient.get()
                    .uri(appointmentsServiceUrl + "/admin/physicians/top")
                    .header("X-INTERNAL-KEY", internalApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.err.println("Erro ao buscar top médicos: " + e.getStatusCode());
            return null;
        } catch (Exception e) {
            System.err.println("Erro genérico ao contactar appointments-service: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtém o relatório mensal de consultas (total, canceladas, reagendadas).
     */
    public String getMonthlyReport(int year, int month) {
        try {
            return webClient.get()
                    .uri(appointmentsServiceUrl + "/admin/appointments/monthly-report?year=" + year + "&month=" + month)
                    .header("X-INTERNAL-KEY", internalApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.err.println("Erro ao obter relatório mensal: " + e.getStatusCode());
            return null;
        } catch (Exception e) {
            System.err.println("Erro genérico: " + e.getMessage());
            return null;
        }
    }

}
