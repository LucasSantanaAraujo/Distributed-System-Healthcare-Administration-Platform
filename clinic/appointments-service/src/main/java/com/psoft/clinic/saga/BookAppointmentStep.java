package com.psoft.clinic.saga;

public enum BookAppointmentStep {
    VALIDATE_PATIENT,
    VALIDATE_PHYSICIAN,
    CHECK_CONFLICTS,
    CREATE_APPOINTMENT,
    PUBLISH_EVENT
}
