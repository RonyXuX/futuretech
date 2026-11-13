package com.futuretech.dto;

import com.futuretech.entity.Category;

/** A category as returned by GET /api/categories. */
public record CategoryDto(Long id, String name, String slug, String description) {

    public static CategoryDto from(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getSlug(), category.getDescription());
    }
}
