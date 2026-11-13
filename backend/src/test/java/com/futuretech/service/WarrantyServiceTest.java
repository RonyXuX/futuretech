package com.futuretech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.futuretech.dto.account.WarrantyDto;
import com.futuretech.entity.Order;
import com.futuretech.entity.OrderItem;
import com.futuretech.entity.Product;
import com.futuretech.entity.Warranty;
import com.futuretech.repository.WarrantyRepository;

@ExtendWith(MockitoExtension.class)
class WarrantyServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @Mock private WarrantyRepository warrantyRepository;

    @Test
    void warranties_haveStatusAndDaysLeft_activeOnesFirst() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC);
        WarrantyService service = new WarrantyService(warrantyRepository, clock);
        when(warrantyRepository.findByOrderItemOrderUserId(5L)).thenReturn(List.of(
                warranty(1L, "Acer Nitro V 15", TODAY.minusYears(2), TODAY.minusYears(1)),        // expired
                warranty(2L, "Forge Pro", TODAY.minusMonths(8), TODAY.plusMonths(16)),            // active
                warranty(3L, "HyperX Cloud III", TODAY.minusMonths(20), TODAY)));                 // last day

        List<WarrantyDto> result = service.getMyWarranties(5L);

        assertThat(result).extracting(WarrantyDto::productName)
                .containsExactly("HyperX Cloud III", "Forge Pro", "Acer Nitro V 15");
        assertThat(result.get(0).status()).isEqualTo(WarrantyDto.Status.ACTIVE);   // expiry day still counts
        assertThat(result.get(0).daysLeft()).isZero();
        assertThat(result.get(1).daysLeft()).isEqualTo(java.time.temporal.ChronoUnit.DAYS.between(TODAY, TODAY.plusMonths(16)));
        assertThat(result.get(2).status()).isEqualTo(WarrantyDto.Status.EXPIRED);
        assertThat(result.get(2).daysLeft()).isZero();
    }

    private static Warranty warranty(Long id, String productName, LocalDate start, LocalDate expiry) {
        Product product = new Product();
        product.setId(id);
        product.setName(productName);
        Order order = new Order();
        order.setId(100 + id);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setOrder(order);
        Warranty w = new Warranty();
        w.setId(id);
        w.setOrderItem(item);
        w.setStartDate(start);
        w.setExpiryDate(expiry);
        return w;
    }
}
