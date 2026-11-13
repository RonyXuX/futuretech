package com.futuretech.repository.spec;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.spec.ComputerSpec;

public interface ComputerSpecRepository extends JpaRepository<ComputerSpec, Long> {
}
