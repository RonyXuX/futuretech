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

/**
 * JpaSpecificationExecutor adds findAll(Specification, Pageable), which lets us build the
 * WHERE clause from whichever filters the user chose (see ProductSpecifications).
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    /**
     * Loads products and LOCKS their rows until the transaction ends (SELECT ... FOR UPDATE).
     * Used at checkout: if two customers buy the last unit at the same moment, the second one
     * waits, then sees stock 0, so we never sell more than we have.
     * Sorted by id so two checkouts always lock rows in the same order (no deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    List<Product> findByProductTypeAndActiveTrue(ProductType productType);

    /**
     * Same as the inherited method, but @EntityGraph loads each product's category in the
     * same query. Without it, showing 20 products would run 20 extra category queries.
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    // Brand lists for the filter dropdown (only brands that have active products)
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true ORDER BY p.brand")
    List<String> findActiveBrands();

    @Query("""
            SELECT DISTINCT p.brand FROM Product p
            WHERE p.active = true AND p.category.slug = :slug
            ORDER BY p.brand""")
    List<String> findActiveBrandsInCategory(@Param("slug") String categorySlug);
}
