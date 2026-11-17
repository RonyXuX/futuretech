package com.futuretech.entity.enums;

/** Motherboard / case size. Declared from smallest to largest on purpose (see fitsIn). */
public enum FormFactor {
    MINI_ITX,
    MICRO_ATX,
    ATX;

    /** A board fits in any case made for its size or bigger: a Mini-ITX board fits in an ATX case. */
    public boolean fitsIn(FormFactor caseMax) {
        return ordinal() <= caseMax.ordinal();
    }
}
