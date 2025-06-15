package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, RevisionRepository<Product, Long, Integer> {
    Optional<Product> findBySlug(String slug);

    org.springframework.data.domain.Page<com.ecommerce.app.model.Product> findByIsPublishedTrue(
            org.springframework.data.domain.Pageable pageable);

    // ... existing methods

    // --- ADDED ---
    /**
     * Counts the number of products associated with a specific category ID.
     * Used to prevent deletion of categories that are in use.
     *
     * @param categoryId The ID of the category.
     * @return The number of products linked to this category.
     */
    @Query("SELECT COUNT(p) FROM Product p JOIN p.categories c WHERE c.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);

}
