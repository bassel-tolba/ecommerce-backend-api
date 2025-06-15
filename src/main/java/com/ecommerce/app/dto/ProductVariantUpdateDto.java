// --- FILE: ProductVariantUpdateDto.java (New File) ---
package com.ecommerce.app.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for updating an existing product variant.
 */
public record ProductVariantUpdateDto(
        @NotNull Long id,
        String sku,
        @Positive BigDecimal price,
        Boolean isActive,
        Set<Long> attributeValueIds) {
}