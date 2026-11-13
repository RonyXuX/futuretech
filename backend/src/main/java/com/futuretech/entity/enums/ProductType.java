package com.futuretech.entity.enums;

/**
 * What kind of product this is. The code relies on it to know which spec table
 * applies and which compatibility rules to run (unlike categories, which are for browsing).
 */
public enum ProductType {
    // PC parts (have a *Spec table, used by ForgeAI)
    CPU,
    MOTHERBOARD,
    RAM,
    GPU,
    STORAGE,
    PSU,
    CASE,
    COOLER,
    // Pre-built computers (have ComputerSpec)
    LAPTOP,
    DESKTOP,
    // No spec table
    ACCESSORY,
    PERIPHERAL
}
