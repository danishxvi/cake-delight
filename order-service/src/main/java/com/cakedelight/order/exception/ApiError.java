package com.cakedelight.order.exception;

import java.time.Instant;

/**
 * Error body returned by every failed request, so clients always receive the
 * same JSON shape.
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
