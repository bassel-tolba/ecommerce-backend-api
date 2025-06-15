// --- FILE: StockAdjustmentDto.java (New File) ---
package com.ecommerce.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for administrative operations to adjust stock levels.
 */
public record StockAdjustmentDto(
        @NotBlank String variantSku,
        @NotNull Long warehouseId,
        @NotNull @Min(1) Integer quantity) {
}