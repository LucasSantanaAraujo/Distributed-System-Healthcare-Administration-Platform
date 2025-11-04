package com.psoft.clinic.client;

import com.psoft.clinic.dto.AppointmentBasicResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AppointmentsServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.appointments-service.url:http://appointments-service:8080}")
    private String appointmentsServiceUrl;

    @Value("${app.internal-api-key:12345}")
    private String internalApiKey;

    public AppointmentBasicResponse getAppointment(Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<AppointmentBasicResponse> response = restTemplate.exchange(
                appointmentsServiceUrl + "/appointments/internal/" + id,
                HttpMethod.GET,
                entity,
                AppointmentBasicResponse.class
        );

        return response.getBody();
    }
}
