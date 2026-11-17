package com.futuretech.entity.enums;

/**
 * Decides which spec table and compatibility rules apply, and the one category the product is
 * listed in. The slugs must match the categories seeded in V2.
 */
public enum ProductType {
    // PC parts (have a *Spec table, used by ForgeAI)
    CPU("processors"),
    MOTHERBOARD("motherboards"),
    RAM("memory"),
    GPU("graphics-cards"),
    STORAGE("storage"),
    PSU("power-supplies"),
    CASE("cases"),
    COOLER("cpu-coolers"),
    // Pre-built computers (have ComputerSpec)
    LAPTOP("laptops"),
    DESKTOP("desktops"),
    // No spec table
    ACCESSORY("accessories"),
    PERIPHERAL("peripherals");

    private final String categorySlug;

    ProductType(String categorySlug) {
        this.categorySlug = categorySlug;
    }

    public String categorySlug() {
        return categorySlug;
    }
}
