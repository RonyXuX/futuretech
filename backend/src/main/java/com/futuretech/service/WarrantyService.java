package com.futuretech.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.account.WarrantyDto;
import com.futuretech.repository.WarrantyRepository;

@Service
public class WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final Clock clock;

    public WarrantyService(WarrantyRepository warrantyRepository, Clock clock) {
        this.warrantyRepository = warrantyRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WarrantyDto> getMyWarranties(Long userId) {
        LocalDate today = LocalDate.now(clock);
        return warrantyRepository.findByOrderItemOrderUserId(userId).stream()
                .map(w -> WarrantyDto.from(w, today))
                .sorted(Comparator.comparing(WarrantyDto::status)            // enum order: ACTIVE first
                        .thenComparing(WarrantyDto::expiryDate))
                .toList();
    }
}
