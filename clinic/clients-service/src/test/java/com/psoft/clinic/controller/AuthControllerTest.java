package com.psoft.clinic.controller;  // Mesmo pacote do controlador real

import com.psoft.clinic.dto.AuthRequest;
import com.psoft.clinic.dto.AuthResponse;
import com.psoft.clinic.dto.PatientRegisterRequest;
import com.psoft.clinic.model.Patient;
import com.psoft.clinic.model.Role;
import com.psoft.clinic.model.User;
import com.psoft.clinic.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)  // Ativa Mockito para mocks
public class AuthControllerTest {

    @Mock
    private UserRepository userRepository;  // Simula o repositório (não usa DB real)

    @InjectMocks
    private AuthController authController;  // Classe testada, com mocks injetados

    // ------------------------------------------------------------------
    //  TESTES PARA /register
    // ------------------------------------------------------------------

    @Test
    public void testRegister_Success() {
        // Arrange: Use dados do DataInitializer (novo paciente) e setters
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente4@clinica.pt");
        request.setPassword("Paciente1234!");
        request.setName("Novo Paciente");
        when(userRepository.existsByEmail("paciente4@clinica.pt")).thenReturn(false);  // Email não existe
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // Act
        ResponseEntity<?> response = authController.register(request, null);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertEquals("TOKEN_TEMPORARIO", authResponse.getToken());
    }

    @Test
    public void testRegister_EmailAlreadyExists() {
        // Arrange: Use email existente do DataInitializer e setters
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente1@clinica.pt");
        request.setPassword("Paciente1234!");
        request.setName("Maria Oliveira");
        when(userRepository.existsByEmail("paciente1@clinica.pt")).thenReturn(true);  // Email já existe

        // Act
        ResponseEntity<?> response = authController.register(request, null);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Email já está registado.", response.getBody());
    }

    @Test
    public void testRegister_InvalidPassword() {
        // Arrange: Senha inválida e setters
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente4@clinica.pt");
        request.setPassword("short");
        request.setName("Novo Paciente");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act
        ResponseEntity<?> response = authController.register(request, null);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Password inválida. Deve ter pelo menos 10 caracteres, incluindo uma maiúscula, um dígito e um caractere especial.", response.getBody());
    }

    // ------------------------------------------------------------------
    //  TESTES PARA /login
    // ------------------------------------------------------------------

    @Test
    public void testLogin_Success() {
        // Arrange: Use dados do DataInitializer e setters
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente1@clinica.pt");
        request.setPassword("Paciente1234!");
        User mockUser = User.builder().email("paciente1@clinica.pt").password("Paciente1234!").build();
        when(userRepository.findByEmail("paciente1@clinica.pt")).thenReturn(Optional.of(mockUser));

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertEquals("TOKEN_TEMPORARIO", authResponse.getToken());
    }

    @Test
    public void testLogin_InvalidCredentials() {
        // Arrange e setters
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente1@clinica.pt");
        request.setPassword("WrongPass123!");
        User mockUser = User.builder().email("paciente1@clinica.pt").password("Paciente1234!").build();
        when(userRepository.findByEmail("paciente1@clinica.pt")).thenReturn(Optional.of(mockUser));

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Credenciais inválidas.", response.getBody());
    }

    @Test
    public void testLogin_UserNotFound() {
        // Arrange e setters
        AuthRequest request = new AuthRequest();
        request.setEmail("nonexistent@clinica.pt");
        request.setPassword("Paciente1234!");
        when(userRepository.findByEmail("nonexistent@clinica.pt")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Credenciais inválidas.", response.getBody());
    }

    // ------------------------------------------------------------------
    //  TESTES PARA /register/patient
    // ------------------------------------------------------------------

    @Test
    public void testRegisterPatient_Success() {
        // Arrange: Use dados do DataInitializer (novo paciente) e setters
        PatientRegisterRequest request = new PatientRegisterRequest();
        request.setEmail("paciente4@clinica.pt");
        request.setPassword("Paciente1234!");
        request.setName("Novo Paciente");
        request.setBirthDate(LocalDate.of(1995, 6, 15));
        request.setPhone("987654321");
        request.setInsuranceInfo("New Insurance");
        request.setConsent(true);
        when(userRepository.existsByEmail("paciente4@clinica.pt")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // Act
        ResponseEntity<?> response = authController.registerPatient(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertEquals("TOKEN_TEMPORARIO", authResponse.getToken());
    }

    @Test
    public void testRegisterPatient_EmailAlreadyExists() {
        // Arrange: Use email existente e setters
        PatientRegisterRequest request = new PatientRegisterRequest();
        request.setEmail("paciente2@clinica.pt");
        request.setPassword("Paciente1234!");
        request.setName("Tiago Mendes");
        request.setBirthDate(LocalDate.of(1985, 2, 20));
        request.setPhone("934567890");
        request.setInsuranceInfo("AdvanceCare");
        request.setConsent(true);
        when(userRepository.existsByEmail("paciente2@clinica.pt")).thenReturn(true);

        // Act
        ResponseEntity<?> response = authController.registerPatient(request);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Email já está registado.", response.getBody());
    }

    @Test
    public void testRegisterPatient_InvalidPassword() {
        // Arrange e setters
        PatientRegisterRequest request = new PatientRegisterRequest();
        request.setEmail("paciente4@clinica.pt");
        request.setPassword("invalid");
        request.setName("Novo Paciente");
        request.setBirthDate(LocalDate.of(1995, 6, 15));
        request.setPhone("987654321");
        request.setInsuranceInfo("New Insurance");
        request.setConsent(true);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act
        ResponseEntity<?> response = authController.registerPatient(request);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Password inválida. Deve ter pelo menos 10 caracteres, incluindo uma maiúscula, um dígito e um caractere especial.", response.getBody());
    }

    // ------------------------------------------------------------------
    //  CENÁRIOS DE BORDA PARA SENHAS
    // ------------------------------------------------------------------

    @Test
    public void testRegister_PasswordTooLong() {
        // Arrange: Senha muito longa (e.g., 1000 chars) - deve passar se atender critérios
        String longPassword = "A1!".repeat(333);  // ~1000 chars, com maiúscula, dígito, especial
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente4@clinica.pt");
        request.setPassword(longPassword);
        request.setName("Novo Paciente");
        when(userRepository.existsByEmail("paciente4@clinica.pt")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // Act
        ResponseEntity<?> response = authController.register(request, null);

        // Assert: Deve passar pois atende critérios
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testRegister_PasswordExactly10Chars() {
        // Arrange: Senha exatamente 10 chars (limite mínimo)
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente4@clinica.pt");
        request.setPassword("A1!2345678");  // 10 chars: maiúscula, dígito, especial
        request.setName("Novo Paciente");
        when(userRepository.existsByEmail("paciente4@clinica.pt")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // Act
        ResponseEntity<?> response = authController.register(request, null);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testRegister_PasswordMissingUppercase() {
        // Arrange: Senha sem maiúscula
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente4@clinica.pt");
        request.setPassword("password123!");  // Sem maiúscula
        request.setName("Novo Paciente");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act
        ResponseEntity<?> response = authController.register(request, null);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Password inválida. Deve ter pelo menos 10 caracteres, incluindo uma maiúscula, um dígito e um caractere especial.", response.getBody());
    }

    @Test
    public void testRegister_PasswordOnlySpecialChars() {
        // Arrange: Senha só com especiais (sem maiúscula/dígito)
        AuthRequest request = new AuthRequest();
        request.setEmail("paciente4@clinica.pt");
        request.setPassword("!!!!!!!!!!");  // 10 chars, só especiais
        request.setName("Novo Paciente");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act
        ResponseEntity<?> response = authController.register(request, null);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
    }
}
