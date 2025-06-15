// --- FILE: AttributeDto.java (Finalized) ---
package com.ecommerce.app.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.ecommerce.app.model.Attribute;

/**
 * DTO representing a product attribute (e.g., "Color") and its possible values.
 */
public record AttributeDto(
        Long id,
        String name,
        List<AttributeValueDto> values) {
    public static AttributeDto toDto(Attribute entity) {
        return new AttributeDto(
                entity.getId(),
                entity.getName(),
                entity.getAttributeValues().stream()
                        .map(AttributeValueDto::toDto)
                        .collect(Collectors.toList()));
    }
}