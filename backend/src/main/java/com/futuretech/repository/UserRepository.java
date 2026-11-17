package com.futuretech.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.futuretech.entity.User;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** One atomic UPDATE: reading, adding and saving in Java could lose points when two orders finish at once. */
    @Modifying
    @Query("UPDATE User u SET u.pointsBalance = u.pointsBalance + :delta WHERE u.id = :userId")
    int addPoints(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * Like addPoints, but changes nothing (returns 0) if the balance would go below 0. The check is in
     * the same UPDATE, so two admins taking points at once can't both pass it.
     * clearAutomatically: the User already loaded in this transaction would otherwise keep the old balance.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.pointsBalance = u.pointsBalance + :delta "
            + "WHERE u.id = :userId AND u.pointsBalance + :delta >= 0")
    int addPointsIfNotNegative(@Param("userId") Long userId, @Param("delta") int delta);
}
