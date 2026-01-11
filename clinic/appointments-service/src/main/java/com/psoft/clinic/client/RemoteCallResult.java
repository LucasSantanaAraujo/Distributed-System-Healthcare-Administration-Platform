package com.psoft.clinic.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RemoteCallResult<T> {
    public enum Status { OK, NOT_FOUND, UNAVAILABLE, FORBIDDEN, BAD_REQUEST }

    private final Status status;
    private final T body;
    private final String error;

    public static <T> RemoteCallResult<T> ok(T body) {
        return new RemoteCallResult<>(Status.OK, body, null);
    }

    public static <T> RemoteCallResult<T> notFound(String msg) {
        return new RemoteCallResult<>(Status.NOT_FOUND, null, msg);
    }

    public static <T> RemoteCallResult<T> unavailable(String msg) {
        return new RemoteCallResult<>(Status.UNAVAILABLE, null, msg);
    }

    public static <T> RemoteCallResult<T> forbidden(String msg) {
        return new RemoteCallResult<>(Status.FORBIDDEN, null, msg);
    }

    public static <T> RemoteCallResult<T> badRequest(String msg) {
        return new RemoteCallResult<>(Status.BAD_REQUEST, null, msg);
    }
}
