package com.psoft.clinic.saga;

public class SagaResult {

    private final boolean success;
    private final String correlationId;
    private final Long appointmentId;
    private final String error;

    public SagaResult(boolean success, String correlationId, Long appointmentId, String error) {
        this.success = success;
        this.correlationId = correlationId;
        this.appointmentId = appointmentId;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public String getError() {
        return error;
    }

    public static SagaResult ok(String correlationId, Long appointmentId) {
        return new SagaResult(true, correlationId, appointmentId, null);
    }

    public static SagaResult fail(String correlationId, String error) {
        return new SagaResult(false, correlationId, null, error);
    }
}
