package com.futuretech.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
}
