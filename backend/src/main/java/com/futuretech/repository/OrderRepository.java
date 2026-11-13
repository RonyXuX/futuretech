package com.futuretech.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.futuretech.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Order history for one customer, newest first. Items are loaded in the same query. */
    @EntityGraph(attributePaths = "items")
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * One order, but only if it belongs to this user. Asking for someone else's order gives
     * "empty", exactly like an order that doesn't exist, so ids can't be probed.
     */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * Same, but locks the order row (used by cancel), so two cancel clicks at the same moment
     * can't both pass the "still PLACED?" check and give the stock back twice.
     * (No EntityGraph here: PostgreSQL can't lock rows of an outer join.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockByIdAndUserId(Long id, Long userId);
}
