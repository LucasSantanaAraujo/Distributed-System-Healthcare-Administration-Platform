package com.psoft.clinic.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianUnavailableEvent {

    private Long physicianId;
    private String reason;
    private String from; // formato YYYY-MM-DD
    private String to;   // formato YYYY-MM-DD
}
