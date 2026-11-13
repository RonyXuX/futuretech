package com.futuretech.dto.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** No prices here on purpose: checkout always uses the server's current prices. */
public record PlaceOrderRequest(
        @NotEmpty @Size(max = 50, message = "can have at most 50 different products")
        List<@Valid @NotNull CartItem> items,

        @NotNull @Valid
        Shipping shipping) {

    public record CartItem(
            @NotNull Long productId,
            @NotNull @Min(1) @Max(value = 10, message = "you can order at most 10 of one product") Integer quantity) {
    }

    public record Shipping(
            @NotBlank @Size(max = 200) String fullName,
            @NotBlank @Size(max = 300) String address,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 30) String phone) {
    }
}
