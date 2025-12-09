package com.psoft.clinic.events;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PhysicianUnavailableRequest {
    private Long physicianId;
    private LocalDate from;
    private LocalDate to;
    private String reason;
}
