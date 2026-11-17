package com.futuretech.forge;

import java.util.List;

/**
 * One compatibility check, in plain Java (no AI). Every rule is a Spring bean, so
 * CompatibilityService gets all of them injected: adding a rule means adding one class.
 *
 * A rule skips itself (returns an empty list) while the parts it compares aren't both chosen yet.
 * That is what makes live checking work while the customer is still picking parts.
 */
public interface CompatibilityRule {

    /** Empty when everything this rule checks is fine. */
    List<Issue> check(BuildParts build);
}
