package com.psoft.clinic.controller;  // Mesmo pacote do controlador real

import com.psoft.clinic.dto.RecordRequest;
import com.psoft.clinic.model.AppointmentRecord;
import com.psoft.clinic.repository.AppointmentRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)  // Ativa Mockito para mocks
public class RecordControllerTest {

    @Mock
    private AppointmentRecordRepository repository;  // Simula o repositório (não usa DB real)

    @InjectMocks
    private RecordController recordController;  // Classe testada, com mocks injetados

    // ------------------------------------------------------------------
    // 🧪 TESTES PARA /records (POST - CREATE)
    // ------------------------------------------------------------------

    @Test
    public void testCreateRecord_Success() {
        // Arrange: Use setters para RecordRequest e builder para AppointmentRecord
        RecordRequest request = new RecordRequest();
        request.setAppointmentId(1L);
        request.setDiagnosis("Diagnosis Test");
        request.setTreatment("Treatment Test");
        request.setPrescription("Prescription Test");

        AppointmentRecord record = AppointmentRecord.builder()
                .appointmentId(1L)
                .diagnosis("Diagnosis Test")
                .treatment("Treatment Test")
                .prescription("Prescription Test")
                .build();

        when(repository.save(any(AppointmentRecord.class))).thenReturn(record);

        // Act
        ResponseEntity<?> response = recordController.createRecord(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        AppointmentRecord responseBody = (AppointmentRecord) response.getBody();
        assertEquals(1L, responseBody.getAppointmentId());
        assertEquals("Diagnosis Test", responseBody.getDiagnosis());
    }

    // ------------------------------------------------------------------
    // 🧪 TESTES PARA /records/{appointmentId} (GET - BY APPOINTMENT)
    // ------------------------------------------------------------------

    @Test
    public void testGetRecordByAppointment_Success() {
        // Arrange
        AppointmentRecord record = AppointmentRecord.builder()
                .id(1L)
                .appointmentId(1L)
                .diagnosis("Diagnosis Test")
                .treatment("Treatment Test")
                .prescription("Prescription Test")
                .build();

        when(repository.findByAppointmentId(1L)).thenReturn(Optional.of(record));

        // Act
        ResponseEntity<?> response = recordController.getRecordByAppointment(1L);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        AppointmentRecord responseBody = (AppointmentRecord) response.getBody();
        assertEquals(1L, responseBody.getAppointmentId());
    }

    @Test
    public void testGetRecordByAppointment_NotFound() {
        // Arrange
        when(repository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = recordController.getRecordByAppointment(1L);

        // Assert
        assertEquals(404, response.getStatusCodeValue());
    }

    // ------------------------------------------------------------------
    // 🧪 CENÁRIOS DE BORDA
    // ------------------------------------------------------------------

    @Test
    public void testCreateRecord_AppointmentIdNull() {
        // Arrange: Cenário de borda - appointmentId null (teste robustez)
        RecordRequest request = new RecordRequest();
        request.setAppointmentId(null);  // Null
        request.setDiagnosis("Diagnosis Test");
        request.setTreatment("Treatment Test");
        request.setPrescription("Prescription Test");

        AppointmentRecord record = AppointmentRecord.builder()
                .appointmentId(null)
                .diagnosis("Diagnosis Test")
                .treatment("Treatment Test")
                .prescription("Prescription Test")
                .build();

        when(repository.save(any(AppointmentRecord.class))).thenReturn(record);

        // Act
        ResponseEntity<?> response = recordController.createRecord(request);

        // Assert: Deve passar (assumindo que o controlador permite null)
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testGetRecordByAppointment_InvalidId() {
        // Arrange: Cenário de borda - ID negativo ou zero
        when(repository.findByAppointmentId(-1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = recordController.getRecordByAppointment(-1L);

        // Assert
        assertEquals(404, response.getStatusCodeValue());
    }
}
