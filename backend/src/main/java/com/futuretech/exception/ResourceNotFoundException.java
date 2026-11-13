package com.futuretech.exception;

/** Thrown when something the client asked for doesn't exist. Turned into HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
