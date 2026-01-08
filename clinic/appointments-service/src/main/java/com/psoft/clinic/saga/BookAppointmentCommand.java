package com.psoft.clinic.saga;

import java.time.LocalDateTime;

public class BookAppointmentCommand {

    private Long patientId;
    private Long physicianId;
    private LocalDateTime dateTime;
    private String reason;
    private String requestedByRole;

    public BookAppointmentCommand(
            Long patientId,
            Long physicianId,
            LocalDateTime dateTime,
            String reason,
            String requestedByRole
    ) {
        this.patientId = patientId;
        this.physicianId = physicianId;
        this.dateTime = dateTime;
        this.reason = reason;
        this.requestedByRole = requestedByRole;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getPhysicianId() {
        return physicianId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getReason() {
        return reason;
    }

    public String getRequestedByRole() {
        return requestedByRole;
    }
}
