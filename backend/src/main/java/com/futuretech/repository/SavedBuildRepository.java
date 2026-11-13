package com.futuretech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.SavedBuild;

public interface SavedBuildRepository extends JpaRepository<SavedBuild, Long> {

    List<SavedBuild> findByUserIdOrderByCreatedAtDesc(Long userId);
}
