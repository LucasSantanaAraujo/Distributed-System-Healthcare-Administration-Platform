package com.psoft.clinic.controller;

import com.psoft.clinic.dto.PhysicianRequest;
import com.psoft.clinic.dto.PhysicianResponse;
import com.psoft.clinic.model.Physician;
import com.psoft.clinic.repository.PhysicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

//  Este controlador pertence ao serviço dos médicos (doctors-service).
//  Foi limpo para não depender do sistema de autenticação nem do serviço de utilizadores.
//  Todos os endpoints que só manipulam médicos (Physician) continuam a funcionar normalmente.

@RestController
@RequestMapping("/physicians")
@RequiredArgsConstructor
public class PhysicianController {

    private final PhysicianRepository physicianRepository;

    //  Estes campos pertenciam ao sistema de autenticação (ficam comentados por agora)
    // private final UserRepository userRepository;       // auth-service
    // private final PasswordEncoder passwordEncoder;     // auth-service
    // private final JwtUtil jwtUtil;                     // auth-service

    // ------------------------------------------------------------------------
    //  ENDPOINTS FUNCIONAIS
    // ------------------------------------------------------------------------

    /**
     * Cria um novo médico (versão simplificada).
     * Não cria utilizador nem token, apenas regista o médico na base de dados.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPhysician(@RequestBody PhysicianRequest request) {
        Physician physician = Physician.builder()
                .name(request.getName())
                .specialty(request.getSpecialty())
                .contact(request.getContact())
                .schedule(request.getSchedule())
                .build();

        physicianRepository.save(physician);
        return ResponseEntity.ok("Médico criado com sucesso (versão simplificada).");
    }

    /**
     * Obtém os dados de um médico pelo seu ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPhysicianById(@PathVariable Long id) {
        return physicianRepository.findById(id)
                .map(physician -> PhysicianResponse.builder()
                        .id(physician.getId())
                        .name(physician.getName())
                        .specialty(physician.getSpecialty())
                        .contact(physician.getContact())
                        .schedule(physician.getSchedule())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Atualiza os dados de um médico.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePhysician(@PathVariable Long id, @RequestBody PhysicianRequest request) {
        return physicianRepository.findById(id)
                .map(physician -> {
                    physician.setName(request.getName());
                    physician.setSpecialty(request.getSpecialty());
                    physician.setContact(request.getContact());
                    physician.setSchedule(request.getSchedule());
                    physicianRepository.save(physician);
                    return ResponseEntity.ok("Dados do médico atualizados com sucesso.");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ------------------------------------------------------------------------
    //  ENDPOINTS DEPENDENTES DE OUTROS SERVIÇOS (comentados temporariamente)
    // ------------------------------------------------------------------------

    /*
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPhysicianWithPhoto(
            @RequestParam("name") String name,
            @RequestParam("specialty") String specialty,
            @RequestParam("contact") String contact,
            @RequestParam("schedule") String schedule,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        //  Este endpoint depende do serviço de autenticação (UserRepository, JwtUtil)
        // e da gestão de ficheiros (armazenamento de foto). Será refeito no futuro
        // com comunicação HTTP entre o doctors-service e o auth-service.

        return ResponseEntity.status(501).body("Endpoint será refeito com comunicação entre serviços.");
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> searchPhysicians(@RequestParam("query") String query) {
        //  Poderá ser integrado com um sistema de pesquisa global ou outro serviço.
        // Por enquanto está desativado.
        return ResponseEntity.status(501).body("Endpoint será refeito futuramente.");
    }
    */

}
