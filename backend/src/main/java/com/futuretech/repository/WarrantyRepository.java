package com.futuretech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.Warranty;

public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    @EntityGraph(attributePaths = {"orderItem", "orderItem.order", "orderItem.product"})
    List<Warranty> findByOrderItemOrderUserId(Long userId);

    List<Warranty> findByOrderItemOrderId(Long orderId);

    void deleteByOrderItemOrderId(Long orderId);
}
