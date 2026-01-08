package com.psoft.clinic.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientCreatedEvent {
    private Long id;
    private Long userId;
    private String name;
    private String contact;
    private LocalDate birthDate;
}