package com.futuretech.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Shared by every paged list (store and admin), so they all treat page and size the same way. */
final class Paging {

    static final int MAX_PAGE_SIZE = 100;

    private Paging() {
    }

    /** Bad page numbers and sizes are corrected instead of rejected. */
    static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), sort);
    }
}
