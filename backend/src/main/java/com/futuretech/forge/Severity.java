package com.futuretech.forge;

public enum Severity {
    /** The parts can't work together. A build with any error is not compatible. */
    ERROR,
    /** It works, but it's risky or not recommended. Doesn't make the build incompatible. */
    WARNING
}
