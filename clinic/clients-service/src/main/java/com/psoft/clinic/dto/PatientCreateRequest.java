package com.psoft.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientCreateRequest {
    private String userId;
    private String name;
    private LocalDate birthDate;
    private String phone;
    private String insuranceInfo;
    private boolean consent; // ✅ campo obrigatório

    // 🔧 Método getter explícito (para garantir compatibilidade)
    public boolean getConsent() {
        return consent;
    }
}
