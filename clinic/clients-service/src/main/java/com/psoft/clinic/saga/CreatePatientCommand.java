package com.psoft.clinic.saga;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreatePatientCommand {
    private String userId;
    private String name;
    private LocalDate birthDate;
    private String phone;
    private String insuranceInfo;
    private Boolean consent;
}
