package com.psoft.clinic.events;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentCreatedEvent {
    private Long appointmentId;
    private Long patientId;
    private Long physicianId;
    private LocalDateTime dateTime;
    private String reason;
}
