package com.futuretech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.Warranty;

public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    // All warranties for one customer (walks warranty -> orderItem -> order -> user).
    // The EntityGraph loads item, order and product in the same query (no extra query per row).
    @EntityGraph(attributePaths = {"orderItem", "orderItem.order", "orderItem.product"})
    List<Warranty> findByOrderItemOrderUserId(Long userId);

    // The warranties of one order (shown next to each line of the order)
    List<Warranty> findByOrderItemOrderId(Long orderId);

    /** A cancelled order has no warranties. (Spring Data builds the delete from the method name.) */
    void deleteByOrderItemOrderId(Long orderId);
}
