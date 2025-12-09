package com.psoft.clinic.controller;

import com.psoft.clinic.config.RabbitMQConfig;
import com.psoft.clinic.events.PhysicianUnavailableEvent;
import com.psoft.clinic.events.PhysicianUnavailableRequest;
import com.psoft.clinic.service.SequenceGeneratorService;
import lombok.extern.slf4j.Slf4j;

import com.psoft.clinic.client.AppointmentsServiceClient;
import com.psoft.clinic.dto.PhysicianCreateRequest;
import com.psoft.clinic.dto.PhysicianRequest;
import com.psoft.clinic.dto.PhysicianResponse;
import com.psoft.clinic.model.Physician;
import com.psoft.clinic.repository.PhysicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/physicians")
public class PhysicianController {

    private final PhysicianRepository physicianRepository;
    private final AppointmentsServiceClient appointmentsServiceClient;
    @Autowired
    private SequenceGeneratorService sequenceGenerator;
    private final RabbitTemplate rabbitTemplate;


    @Value("${internal.api.key}")
    private String internalApiKey;

    @PostMapping("/unavailable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> markPhysicianUnavailable(@RequestBody PhysicianUnavailableRequest req) {

        PhysicianUnavailableEvent event = new PhysicianUnavailableEvent(
                req.getPhysicianId(),
                req.getReason(),
                req.getFrom().toString(),
                req.getTo().toString()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCTORS_EXCHANGE,
                RabbitMQConfig.PHYSICIAN_UNAVAILABLE_ROUTING_KEY,
                event
        );

        return ResponseEntity.ok("Indisponibilidade enviada via RabbitMQ.");
    }







    // ============================================================
    // INTERNAL (auth-service) → cria Physician
    // ============================================================
    @PostMapping("/internal")
    public ResponseEntity<?> createPhysicianInternal(@RequestBody PhysicianCreateRequest request) {



        if (physicianRepository.findByUserId(request.getUserId()).isPresent()) {
            return ResponseEntity.ok("Médico já existente.");
        }

        Physician physician = new Physician();

        physician.setId(sequenceGenerator.generateSequence("physician_sequence"));
        physician.setUserId(request.getUserId());
        physician.setName(request.getName());
        physician.setSpecialty(request.getSpecialty());
        physician.setContact(request.getContact());
        physician.setSchedule(request.getSchedule());
        physician.setPhotoPath(request.getPhotoPath());

        physicianRepository.save(physician);


        log.info("💾 Médico criado no doctors-service: {}", physician.getId());
        return ResponseEntity.ok("Médico criado com sucesso!");
    }

    // ============================================================
    // INTERNAL (appointments-service) → buscar por ID
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getPhysicianById(
            @PathVariable Long id,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String internalKey,
            Principal principal
    ) {

        boolean isInternal = internalKey != null && internalKey.equals(internalApiKey);

        if (!isInternal && principal == null) {
            return ResponseEntity.status(403).body("Acesso negado.");
        }

        return physicianRepository.findById(id)
                .map(p -> {
                    // Resposta completa (apenas para serviços internos)
                    if (isInternal) {
                        return ResponseEntity.ok((Object) p);
                    }

                    // Resposta limitada (Frontend / Admin)
                    PhysicianResponse dto = PhysicianResponse.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .specialty(p.getSpecialty())
                            .contact(p.getContact())
                            .schedule(p.getSchedule())
                            .build();

                    return ResponseEntity.ok((Object) dto);
                })
                .orElse(ResponseEntity.status(404).body("Médico não encontrado."));

    }

    // ============================================================
    // PATIENT → pesquisar médicos
    // ============================================================
    @GetMapping("/search")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> search(@RequestParam("query") String query) {

        var physicians = physicianRepository
                .findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(query, query);

        var responses = physicians.stream().map(p ->
                PhysicianResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .specialty(p.getSpecialty())
                        .contact(p.getContact())
                        .schedule(p.getSchedule())
                        .build()
        ).toList();

        return ResponseEntity.ok(responses);
    }


    // ============================================================
    // ADMIN → atualizar médico
    // ============================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePhysician(
            @PathVariable Long id,
            @RequestBody PhysicianRequest request
    ) {

        return physicianRepository.findById(id)
                .map(physician -> {
                    physician.setName(request.getName());
                    physician.setSpecialty(request.getSpecialty());
                    physician.setContact(request.getContact());
                    physician.setSchedule(request.getSchedule());
                    physicianRepository.save(physician);
                    return ResponseEntity.ok("Dados atualizados com sucesso.");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================
    // ADMIN → estatísticas
    // ============================================================
    @GetMapping("/admin/only")
    @PreAuthorize("hasRole('ADMIN')")
    public String onlyForAdmins() {
        return "Bem-vindo, ADMIN!";
    }

    @GetMapping("/admin/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTopPhysicians() {
        try {
            return ResponseEntity.ok(appointmentsServiceClient.getTopPhysiciansByAppointmentCount());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao obter ranking: " + e.getMessage());
        }
    }

    @GetMapping("/admin/average-duration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAverageDuration() {
        Map<String, Integer> map = new HashMap<>();

        physicianRepository.findAll()
                .forEach(p -> map.put(p.getName(), 30)); // Dummy para o projeto

        return ResponseEntity.ok(map);
    }

    @GetMapping("/admin/monthly-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month
    ) {
        try {
            return ResponseEntity.ok(appointmentsServiceClient.getMonthlyReport(year, month));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao obter relatório: " + e.getMessage());
        }
    }
}
