package com.ecommerce.app.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import com.ecommerce.app.model.ProductVariant;

// FIX: Removed @Builder from record.
public record ProductVariantDto(
        Long id,
        Long productId,
        String sku,
        BigDecimal price,
        BigDecimal weightKg,
        boolean isActive,
        Map<String, String> attributes) {
    public static ProductVariantDto toDto(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        return new ProductVariantDto(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getSku(),
                variant.getPrice(),
                variant.getWeightKg(),
                variant.isActive(),
                variant.getAttributeValues().stream()
                        .collect(Collectors.toMap(
                                av -> av.getAttribute().getName(),
                                av -> av.getValue())));
    }

    public ProductVariant toEntity() {
        ProductVariant pv = new ProductVariant();
        pv.setId(this.id);
        pv.setSku(this.sku);
        pv.setPrice(this.price);
        pv.setWeightKg(this.weightKg);
        pv.setActive(this.isActive);
        return pv;
    }
}
