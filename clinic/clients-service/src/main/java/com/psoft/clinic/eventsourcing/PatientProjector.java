package com.psoft.clinic.eventsourcing;

import com.psoft.clinic.model.Patient;
import com.psoft.clinic.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientProjector {

    private final PatientRepository repository;

    public void project(PatientEvent event) {
        if ("PATIENT_CREATED".equals(event.getEventType())) {
            Map<String, Object> p = event.getPayload();

            Patient patient = new Patient();
            patient.setId(event.getAggregateId());
            // Conversões seguras de tipos vindos do JSON Map
            patient.setUserId(p.get("userId") != null ? p.get("userId").toString() : null);
            patient.setName((String) p.get("name"));
            // Converter String/Long para LocalDate (depende da serialização do Jackson)
            // Assumindo que vem como String ISO ou array, ajusta conforme o teu Jackson config
            if (p.get("birthDate") != null) {
                patient.setBirthDate(LocalDate.parse(p.get("birthDate").toString()));
            }
            patient.setContact((String) p.get("phone"));
            patient.setInsuranceInfo((String) p.get("insuranceInfo"));
            patient.setConsent((Boolean) p.get("consent"));

            repository.save(patient);
        }
    }
}
