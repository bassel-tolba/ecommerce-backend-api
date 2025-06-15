// --- FILE: AttributeRepository.java (New or Updated) ---
package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.Attribute;

/**
 * Spring Data JPA repository for the {@link Attribute} entity.
 */
@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {

    /**
     * Finds an attribute by its unique name.
     *
     * @param name The name of the attribute to find.
     * @return An {@link Optional} containing the found attribute, or empty if not
     *         found.
     */
    Optional<Attribute> findByName(String name);

    /**
     * Checks if an attribute with the given name exists.
     * This is more efficient than fetching the full entity if only existence is
     * needed.
     *
     * @param name The name of the attribute to check.
     * @return {@code true} if an attribute with the given name exists,
     *         {@code false} otherwise.
     */
    boolean existsByName(String name);
}