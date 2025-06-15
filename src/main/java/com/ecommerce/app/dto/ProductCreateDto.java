package com.ecommerce.app.dto;
import java.math.BigDecimal;
import java.util.Set;
// DTO for creating a new Product with its variants
public record ProductCreateDto(
    String name,
    String description,
    BigDecimal basePrice,
    Set<Long> categoryIds,
    Set<ProductVariantCreateDto> variants
) {}
