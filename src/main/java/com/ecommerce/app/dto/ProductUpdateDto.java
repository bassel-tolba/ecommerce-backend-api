// --- FILE: ProductUpdateDto.java (New File) ---
package com.ecommerce.app.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for a comprehensive, granular update of a product and its variants.
 */
public record ProductUpdateDto(
        // Top-level product fields
        String name,
        String description,
        BigDecimal basePrice,
        Set<Long> categoryIds,

        // Granular variant management
        @NotNull List<ProductVariantUpdateDto> variantsToUpdate,
        @NotNull List<ProductVariantCreateDto> variantsToCreate,
        @NotNull Set<Long> variantIdsToDelete) {
}