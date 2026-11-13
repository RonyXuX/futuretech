package com.futuretech.dto;

import org.springframework.data.domain.Sort;

/** Values must be sent in capitals, e.g. ?sort=PRICE_ASC. */
public enum ProductSort {

    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "price")),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "price")),
    NAME(Sort.by(Sort.Direction.ASC, "name")),
    // nullsLast: products without a score (peripherals, accessories) go to the end, not the top
    PERFORMANCE(Sort.by(Sort.Order.desc("performanceScore").nullsLast()));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    /** Adds the id as a tie-breaker so paging is stable when two products have equal values. */
    public Sort toSort() {
        return sort.and(Sort.by("id"));
    }
}
