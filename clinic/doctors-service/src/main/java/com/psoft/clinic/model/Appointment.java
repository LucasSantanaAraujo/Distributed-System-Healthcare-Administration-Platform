package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidade Appointment (Consulta)
 * Pertence ao doctors-service.
 *
 * As relações com Patient e AppointmentRecord são agora lógicas,
 * e serão tratadas através de comunicação HTTP com outros serviços.
 */

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateTime;

    private String reason;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    // 🔸 Antes: relação ManyToOne direta com Patient (de outro serviço)
    // @ManyToOne
    // @JoinColumn(name = "patient_id")
    // private Patient patient;
    //
    // 🔹 Agora substituímos por apenas o ID do paciente
    //    — o resto será obtido via HTTP do clients-service.
    private Long patientId;

    // ✅ Médico pertence a este serviço, portanto mantém-se a relação
    @ManyToOne
    @JoinColumn(name = "physician_id")
    private Physician physician;

    // 🔸 Antes: relação OneToOne com AppointmentRecord (outro serviço)
    // @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL)
    // private AppointmentRecord record;
    //
    // 🔹 Agora usamos apenas um ID simbólico (para referência cruzada futura)
    private Long recordId;
}
