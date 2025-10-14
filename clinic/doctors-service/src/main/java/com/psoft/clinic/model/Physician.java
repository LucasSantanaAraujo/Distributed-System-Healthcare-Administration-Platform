package com.psoft.clinic.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Physician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String specialty;

    private String contact;

    private String schedule;

    // 🔸 O campo 'photoPath' pode manter-se (opcional)
    private String photoPath;

    // ❌ Antes existia:
    // @OneToOne(mappedBy = "physician")
    // private User user;
    //
    // 🔸 Esse campo foi removido porque a classe User pertence a outro serviço
    // (auth-service ou clients-service). Se for necessário, mais tarde iremos
    // comunicar via HTTP para associar médico a utilizador.
}
