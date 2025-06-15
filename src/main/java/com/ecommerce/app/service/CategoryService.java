// --- FILE: CategoryService.java ---
package com.ecommerce.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.app.dto.CategoryDto;
import com.ecommerce.app.dto.CategoryRequestDto;
import com.ecommerce.app.exception.InvalidOperationException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.Category;
import com.ecommerce.app.repository.CategoryRepository;
import com.ecommerce.app.repository.ProductRepository;
import com.github.slugify.Slugify;

import lombok.RequiredArgsConstructor;

/**
 * Service class for managing product categories.
 *
 * Provides functionalities for creating, retrieving, updating, and safely
 * deleting categories.
 * Ensures data integrity by preventing deletion of categories that are in use.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository; // For dependency checks
    private final Slugify slugify = Slugify.builder().build();

    /**
     * Creates a new category.
     *
     * @param requestDto The DTO containing the data for the new category.
     * @return A CategoryDto representing the newly created category.
     * @throws ResourceNotFoundException if the specified parent category does not
     *                                   exist.
     */
    @Transactional
    public CategoryDto createCategory(CategoryRequestDto requestDto) {
        Category parent = findParentCategory(requestDto.parentCategoryId());

        Category category = Category.builder()
                .name(requestDto.name())
                .slug(slugify.slugify(requestDto.name()))
                .parentCategory(parent)
                .build();

        return CategoryDto.toDto(categoryRepository.save(category));
    }

    /**
     * Updates an existing category's name and/or parent.
     *
     * @param categoryId The ID of the category to update.
     * @param requestDto The DTO containing the updated data.
     * @return A CategoryDto representing the updated category.
     * @throws ResourceNotFoundException if the category or new parent category does
     *                                   not exist.
     * @throws InvalidOperationException if an attempt is made to make a category
     *                                   its own parent.
     */
    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryRequestDto requestDto) {
        Category categoryToUpdate = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        if (categoryId.equals(requestDto.parentCategoryId())) {
            throw new InvalidOperationException("A category cannot be its own parent.");
        }

        Category parent = findParentCategory(requestDto.parentCategoryId());

        categoryToUpdate.setName(requestDto.name());
        categoryToUpdate.setSlug(slugify.slugify(requestDto.name()));
        categoryToUpdate.setParentCategory(parent);

        return CategoryDto.toDto(categoryRepository.save(categoryToUpdate));
    }

    /**
     * Deletes a category safely.
     * The deletion is prevented if the category has child categories or is assigned
     * to any products.
     *
     * @param categoryId The ID of the category to delete.
     * @throws ResourceNotFoundException if the category does not exist.
     * @throws InvalidOperationException if the category is in use and cannot be
     *                                   deleted.
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }

        // Check for child categories
        if (categoryRepository.existsByParentCategoryId(categoryId)) {
            throw new InvalidOperationException("Cannot delete category: It has one or more child categories.");
        }

        // Check for associated products
        if (productRepository.countByCategoryId(categoryId) > 0) {
            throw new InvalidOperationException("Cannot delete category: It is associated with one or more products.");
        }

        categoryRepository.deleteById(categoryId);
    }

    /**
     * Retrieves all categories.
     *
     * @return A list of all categories as DTOs.
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> findAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryDto::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to find and validate the parent category.
     *
     * @param parentId The ID of the parent category (can be null).
     * @return The parent Category entity, or null if parentId is null.
     * @throws ResourceNotFoundException if parentId is not null but the category is
     *                                   not found.
     */
    private Category findParentCategory(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", parentId));
    }
}