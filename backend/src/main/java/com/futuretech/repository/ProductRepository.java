package com.futuretech.repository;

import java.util.List;
import java.util.Optional;

import com.futuretech.entity.enums.ProductType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByProductTypeAndActiveTrue(ProductType productType);
}
