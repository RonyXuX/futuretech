package com.futuretech.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.ProductType;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    /**
     * SELECT ... FOR UPDATE: a second checkout of the same product waits, then sees the new stock,
     * so we never oversell. Ordered by id so checkouts always lock in the same order (no deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    List<Product> findByProductTypeAndActiveTrue(ProductType productType);

    // Loads the category in the same query (otherwise one extra query per product)
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true ORDER BY p.brand")
    List<String> findActiveBrands();

    @Query("""
            SELECT DISTINCT p.brand FROM Product p
            WHERE p.active = true AND p.category.slug = :slug
            ORDER BY p.brand""")
    List<String> findActiveBrandsInCategory(@Param("slug") String categorySlug);
}
