package com.ecommerce.app.dto;
import com.ecommerce.app.model.Category;
// DTO for Category CRUD
public record CategoryDto(Long id, String name, String slug, Long parentCategoryId) {
    public static CategoryDto toDto(Category category) {
        if (category == null) return null;
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getParentCategory() != null ? category.getParentCategory().getId() : null
        );
    }
}
