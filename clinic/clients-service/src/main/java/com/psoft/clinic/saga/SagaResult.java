package com.psoft.clinic.saga;

import lombok.Getter;

@Getter
public class SagaResult {

    private final boolean success;
    private final String correlationId;
    private final Long entityId; // ID do paciente criado
    private final String error;

    public SagaResult(boolean success, String correlationId, Long entityId, String error) {
        this.success = success;
        this.correlationId = correlationId;
        this.entityId = entityId;
        this.error = error;
    }

    public static SagaResult ok(String correlationId, Long entityId) {
        return new SagaResult(true, correlationId, entityId, null);
    }

    public static SagaResult fail(String correlationId, String error) {
        return new SagaResult(false, correlationId, null, error);
    }
}
