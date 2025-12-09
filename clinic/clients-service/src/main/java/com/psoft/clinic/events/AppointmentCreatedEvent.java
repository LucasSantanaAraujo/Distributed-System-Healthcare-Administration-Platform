package com.psoft.clinic.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCreatedEvent {

    private Long appointmentId;
    private Long patientId;
    private Long physicianId;
    private LocalDateTime dateTime;
    private String reason;
}
