package com.ecommerce.app.dto;
import java.math.BigDecimal;
// Nested DTO for creating variants
public record ProductVariantCreateDto(String sku, BigDecimal price) {}
