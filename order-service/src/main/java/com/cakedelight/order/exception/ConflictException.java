package com.cakedelight.order.exception;

/**
 * The request is valid but conflicts with the current state of the order,
 * for example changing a completed order or checking out an empty basket.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
