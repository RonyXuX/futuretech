package com.futuretech.forge;

import java.util.List;

/**
 * One problem found by a rule.
 *
 * @param rule       which rule found it, e.g. "SOCKET" (the frontend and the LLM can use it as a key)
 * @param message    a sentence a customer understands, naming the parts involved
 * @param productIds the parts involved, so the builder can highlight them
 */
public record Issue(String rule, Severity severity, String message, List<Long> productIds) {

    public static Issue error(String rule, String message, Long... productIds) {
        return new Issue(rule, Severity.ERROR, message, List.of(productIds));
    }

    public static Issue warning(String rule, String message, Long... productIds) {
        return new Issue(rule, Severity.WARNING, message, List.of(productIds));
    }
}
