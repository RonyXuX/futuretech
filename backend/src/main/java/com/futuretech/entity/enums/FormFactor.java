package com.futuretech.entity.enums;

/**
 * Motherboard / case size. Declared from smallest to largest on purpose:
 * a board fits in a case when board.ordinal() <= caseMax.ordinal().
 */
public enum FormFactor {
    MINI_ITX,
    MICRO_ATX,
    ATX
}
