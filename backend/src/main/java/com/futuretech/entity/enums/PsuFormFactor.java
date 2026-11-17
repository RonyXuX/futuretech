package com.futuretech.entity.enums;

/** Power supply size. Declared from smallest to largest on purpose, like FormFactor (see fitsIn). */
public enum PsuFormFactor {
    SFX,
    ATX;

    /** An SFX power supply also fits where an ATX one does; an ATX one doesn't fit an SFX-only case. */
    public boolean fitsIn(PsuFormFactor caseMax) {
        return ordinal() <= caseMax.ordinal();
    }
}
