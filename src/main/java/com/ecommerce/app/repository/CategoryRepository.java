package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);

    // --- ADDED ---
    /**
     * Checks if any category exists with the given parent category ID.
     * Used to determine if a category has children.
     *
     * @param parentId The ID of the potential parent category.
     * @return true if child categories exist, false otherwise.
     */
    boolean existsByParentCategoryId(Long parentId);
}
