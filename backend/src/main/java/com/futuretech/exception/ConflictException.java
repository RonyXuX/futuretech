package com.futuretech.exception;

import java.util.List;

/** HTTP 409. "problems" lists every reason, so the cart page can show them all at once. */
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
