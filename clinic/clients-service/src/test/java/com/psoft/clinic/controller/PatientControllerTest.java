package com.psoft.clinic.controller;  // Mesmo pacote do controlador real

import com.psoft.clinic.dto.PatientResponse;
import com.psoft.clinic.dto.PatientUpdateRequest;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.PatientRepository;
import com.psoft.clinic.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)  // Ativa Mockito para mocks
public class PatientControllerTest {

    @Mock
    private UserRepository userRepository;  // Simula o repositório de usuários

    @Mock
    private PatientRepository patientRepository;  // Simula o repositório de pacientes

    @InjectMocks
    private PatientController patientController;  // Classe testada, com mocks injetados

    // ------------------------------------------------------------------
    //  TESTES PARA /patients (GET ALL)
    // ------------------------------------------------------------------

    @Test
    public void testGetAllPatients_Success() {
        // Arrange: Mock usuários com pacientes
        User user1 = User.builder().email("paciente1@clinica.pt").build();
        Patient patient1 = Patient.builder()
                .id(1L)
                .name("Maria Oliveira")
                .birthDate(LocalDate.of(1990, 5, 10))
                .contact("912345678")
                .insuranceInfo("Multicare")
                .user(user1)
                .build();
        user1.setPatient(patient1);

        when(userRepository.findAll()).thenReturn(List.of(user1));

        // Act
        ResponseEntity<List<PatientResponse>> response = patientController.getAllPatients();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Maria Oliveira", response.getBody().get(0).getName());
    }

    @Test
    public void testGetAllPatients_Empty() {
        // Arrange: Nenhum usuário com paciente
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        ResponseEntity<List<PatientResponse>> response = patientController.getAllPatients();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(0, response.getBody().size());
    }

    // ------------------------------------------------------------------
    //  TESTES PARA /patients/{id} (GET BY ID)
    // ------------------------------------------------------------------

    @Test
    public void testGetPatientById_Success() {
        // Arrange
        Patient patient = Patient.builder()
                .id(1L)
                .name("Maria Oliveira")
                .birthDate(LocalDate.of(1990, 5, 10))
                .contact("912345678")
                .insuranceInfo("Multicare")
                .user(User.builder().email("paciente1@clinica.pt").build())
                .build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        // Act
        ResponseEntity<?> response = patientController.getPatientById(1L);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        PatientResponse resp = (PatientResponse) response.getBody();
        assertEquals("Maria Oliveira", resp.getName());
    }

    @Test
    public void testGetPatientById_NotFound() {
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = patientController.getPatientById(1L);

        // Assert
        assertEquals(404, response.getStatusCodeValue());
    }

    // ------------------------------------------------------------------
    //   TESTES PARA /patients/search (SEARCH BY NAME)
    // ------------------------------------------------------------------

    @Test
    public void testSearchPatientsByName_Success() {
        // Arrange
        Patient patient = Patient.builder()
                .id(1L)
                .name("Maria Oliveira")
                .birthDate(LocalDate.of(1990, 5, 10))
                .contact("912345678")
                .insuranceInfo("Multicare")
                .user(User.builder().email("paciente1@clinica.pt").build())
                .build();
        when(patientRepository.findByNameContainingIgnoreCase("Maria")).thenReturn(List.of(patient));

        // Act
        ResponseEntity<?> response = patientController.searchPatientsByName("Maria");

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        List<PatientResponse> resp = (List<PatientResponse>) response.getBody();
        assertEquals(1, resp.size());
        assertEquals("Maria Oliveira", resp.get(0).getName());
    }

    @Test
    public void testSearchPatientsByName_NoMatches() {
        // Arrange
        when(patientRepository.findByNameContainingIgnoreCase("Inexistente")).thenReturn(List.of());

        // Act
        ResponseEntity<?> response = patientController.searchPatientsByName("Inexistente");

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        List<PatientResponse> resp = (List<PatientResponse>) response.getBody();
        assertEquals(0, resp.size());
    }

    // ------------------------------------------------------------------
    //  TESTES PARA /patients/{id} (UPDATE)
    // ------------------------------------------------------------------

    @Test
    public void testUpdateMyData_Success() {
        // Arrange
        Patient patient = Patient.builder()
                .id(1L)
                .name("Maria Oliveira")
                .contact("912345678")
                .insuranceInfo("Multicare")
                .build();
        PatientUpdateRequest request = new PatientUpdateRequest();
        request.setName("Maria Silva");
        request.setPhone("987654321");
        request.setInsuranceInfo("NewCare");
        request.setPhotoBase64("base64data");
        request.setHealthConcerns(List.of("Alergia"));

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        // Act
        ResponseEntity<?> response = patientController.updateMyData(1L, request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Patient updated successfully", response.getBody());
    }

    @Test
    public void testUpdateMyData_NotFound() {
        // Arrange
        PatientUpdateRequest request = new PatientUpdateRequest();
        request.setName("Novo Nome");
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = patientController.updateMyData(1L, request);

        // Assert
        assertEquals(404, response.getStatusCodeValue());
    }
}
