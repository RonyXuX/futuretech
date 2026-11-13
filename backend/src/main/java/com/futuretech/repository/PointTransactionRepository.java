package com.futuretech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.futuretech.entity.PointTransaction;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    // newest first; the id breaks ties between entries made at the same moment
    List<PointTransaction> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    /** Net points an order is currently worth: earned at checkout minus anything taken back. */
    @Query("SELECT COALESCE(SUM(t.points), 0) FROM PointTransaction t WHERE t.order.id = :orderId")
    int sumForOrder(@Param("orderId") Long orderId);
}
