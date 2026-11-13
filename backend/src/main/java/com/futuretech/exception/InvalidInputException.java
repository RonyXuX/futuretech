package com.futuretech.exception;

/**
 * A value is well-formed but wrong, e.g. the "current password" doesn't match. Becomes 400 with
 * the same {"errors": {field: message}} shape as validation errors, so forms handle both alike.
 */
public class InvalidInputException extends RuntimeException {

    private final String field;

    public InvalidInputException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
