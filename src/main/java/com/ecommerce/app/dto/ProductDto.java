package com.ecommerce.app.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import com.ecommerce.app.model.Product;

// FIX: Removed @Builder from record.
public record ProductDto(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal basePrice,
        boolean isPublished,
        Set<ProductVariantDto> variants,
        Set<String> categories) {
    public static ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getBasePrice(),
                product.isPublished(),
                product.getVariants().stream().map(ProductVariantDto::toDto).collect(Collectors.toSet()),
                product.getCategories().stream().map(c -> c.getName()).collect(Collectors.toSet()));
    }

    public Product toEntity() {
        Product p = new Product();
        p.setId(this.id);
        p.setName(this.name);
        p.setSlug(this.slug);
        p.setDescription(this.description);
        p.setBasePrice(this.basePrice);
        p.setPublished(this.isPublished);
        return p;
    }
}
