// --- FILE: AttributeValueRepository.java (New File) ---
package com.ecommerce.app.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.AttributeValue;

/**
 * Spring Data JPA repository for the {@link AttributeValue} entity.
 */
@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {

    /**
     * Finds a set of AttributeValue entities by their IDs.
     * This is useful for validating and fetching a collection of values at once.
     *
     * @param ids A set of AttributeValue IDs.
     * @return A list of found {@link AttributeValue} entities.
     */
    List<AttributeValue> findByIdIn(Set<Long> ids);
}