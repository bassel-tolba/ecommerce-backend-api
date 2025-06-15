// --- FILE: ProductVariantRepository.java (Updated) ---
package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.ProductVariant;

/**
 * Spring Data JPA repository for the {@link ProductVariant} entity.
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Finds a product variant by its unique SKU (Stock Keeping Unit).
     *
     * @param sku The SKU to search for.
     * @return An {@link Optional} containing the found product variant, or empty if
     *         not found.
     */
    Optional<ProductVariant> findBySku(String sku);

    /**
     * Checks if a product variant with the given SKU exists.
     *
     * @param sku The SKU to check.
     * @return {@code true} if a variant with the given SKU exists, {@code false}
     *         otherwise.
     */
    boolean existsBySku(String sku);
}