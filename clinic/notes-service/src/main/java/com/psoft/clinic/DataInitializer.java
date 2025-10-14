package com.psoft.clinic;

import com.psoft.clinic.model.AppointmentRecord;
import com.psoft.clinic.repository.AppointmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AppointmentRecordRepository recordRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Limpar base de dados anterior (opcional)
            recordRepository.deleteAll();

            // Criar alguns registos médicos de exemplo
            AppointmentRecord r1 = AppointmentRecord.builder()
                    .appointmentId(1L)
                    .diagnosis("Gripe comum")
                    .treatment("Repouso, líquidos e antipirético")
                    .prescription("Paracetamol 500mg de 8h em 8h")
                    .build();

            AppointmentRecord r2 = AppointmentRecord.builder()
                    .appointmentId(2L)
                    .diagnosis("Enxaqueca")
                    .treatment("Reduzir exposição à luz e descanso")
                    .prescription("Ibuprofeno 400mg se necessário")
                    .build();

            AppointmentRecord r3 = AppointmentRecord.builder()
                    .appointmentId(3L)
                    .diagnosis("Alergia sazonal")
                    .treatment("Evitar pólen e usar anti-histamínico")
                    .prescription("Cetirizina 10mg à noite")
                    .build();

            recordRepository.save(r1);
            recordRepository.save(r2);
            recordRepository.save(r3);

            System.out.println("✅ Registos médicos de exemplo criados com sucesso!");
        };
    }
}
