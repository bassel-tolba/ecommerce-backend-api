// --- FILE: ProductVariantCreateDto.java (Updated) ---
package com.ecommerce.app.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductVariantCreateDto(
        @NotBlank String sku,
        @NotNull @Positive BigDecimal price,
        // Add other fields like weight if needed
        @NotNull Set<Long> attributeValueIds) {
}