// --- FILE: CategoryRequestDto.java (New File) ---
package com.ecommerce.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A DTO for handling category creation and update requests.
 *
 * @param name             The name of the category.
 * @param parentCategoryId The ID of the parent category, which can be null for
 *                         a top-level category.
 */
public record CategoryRequestDto(
        @NotBlank @Size(max = 100, message = "Category name cannot exceed 100 characters.") String name,
        Long parentCategoryId) {
}