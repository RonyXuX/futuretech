package com.futuretech.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Used instead of Spring's Page, whose JSON format is not a stable API. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
