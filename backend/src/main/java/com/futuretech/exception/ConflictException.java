package com.futuretech.exception;

import java.util.List;

/**
 * The request is valid, but the current state doesn't allow it (HTTP 409), e.g. not enough
 * stock, or cancelling an order that already shipped. "problems" lists each reason, so the
 * cart page can show them all at once.
 */
public class ConflictException extends RuntimeException {

    private final List<String> problems;

    public ConflictException(String message) {
        this(message, List.of());
    }

    public ConflictException(String message, List<String> problems) {
        super(message);
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }
}
