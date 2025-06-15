// --- FILE: AttributeValueDto.java (New File) ---
package com.ecommerce.app.dto;

import com.ecommerce.app.model.AttributeValue;

/**
 * DTO representing a specific value for an attribute (e.g., "Red" for "Color").
 */
public record AttributeValueDto(
        Long id,
        String value) {
    public static AttributeValueDto toDto(AttributeValue entity) {
        return new AttributeValueDto(entity.getId(), entity.getValue());
    }
}