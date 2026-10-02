package com.cakedelight.rating.exception;

/**
 * A downstream service could not be reached even after retries.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
