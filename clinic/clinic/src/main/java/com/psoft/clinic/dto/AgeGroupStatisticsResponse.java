package com.psoft.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgeGroupStatisticsResponse {
    private String ageGroup;
    private int appointmentCount;
    private double averageDurationMinutes;
}
