package com.futuretech.entity.enums;

/** Decides which spec table and compatibility rules apply. Categories are only for browsing. */
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
