package com.futuretech.entity.enums;

/**
 * Power supply size. Declared from smallest to largest on purpose, like FormFactor:
 * a PSU fits in a case when psu.ordinal() <= caseMax.ordinal().
 */
public enum PsuFormFactor {
    SFX,
    ATX
}
