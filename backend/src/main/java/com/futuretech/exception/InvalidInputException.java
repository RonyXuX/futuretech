package com.futuretech.exception;

/** A well-formed but wrong value (e.g. wrong current password): 400 on that field. */
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
