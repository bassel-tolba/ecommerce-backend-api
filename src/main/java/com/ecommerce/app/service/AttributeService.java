// --- FILE: AttributeService.java (New File) ---
package com.ecommerce.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.app.dto.AttributeDto;
import com.ecommerce.app.exception.DuplicateResourceException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.Attribute;
import com.ecommerce.app.model.AttributeValue;
import com.ecommerce.app.repository.AttributeRepository;
import com.ecommerce.app.repository.AttributeValueRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class for managing product attributes and their values.
 *
 * Provides functionalities for creating attributes (e.g., "Color", "Size")
 * and adding distinct values to them (e.g., "Red", "Large").
 */
@Service
@RequiredArgsConstructor
public class AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;

    /**
     * Creates a new attribute type.
     *
     * @param name The name of the attribute (e.g., "Color"). Must be unique.
     * @return An AttributeDto representing the newly created attribute.
     * @throws DuplicateResourceException if an attribute with the same name already
     *                                    exists.
     */
    @Transactional
    public AttributeDto createAttribute(String name) {
        if (attributeRepository.existsByName(name)) {
            throw new DuplicateResourceException("Attribute", "name", name);
        }
        Attribute attribute = Attribute.builder().name(name).build();
        return AttributeDto.toDto(attributeRepository.save(attribute));
    }

    /**
     * Adds a new value to an existing attribute.
     *
     * @param attributeId The ID of the attribute to which the value will be added.
     * @param value       The new value to add (e.g., "Red"). Must be unique for the
     *                    given attribute.
     * @return An AttributeDto representing the parent attribute with its updated
     *         list of values.
     * @throws ResourceNotFoundException  if the attribute does not exist.
     * @throws DuplicateResourceException if the value already exists for this
     *                                    attribute.
     */
    @Transactional
    public AttributeDto addAttributeValue(Long attributeId, String value) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute", "id", attributeId));

        boolean valueExists = attribute.getAttributeValues().stream()
                .anyMatch(v -> v.getValue().equalsIgnoreCase(value));

        if (valueExists) {
            throw new DuplicateResourceException("AttributeValue", "value", value);
        }

        AttributeValue newAttributeValue = AttributeValue.builder()
                .attribute(attribute)
                .value(value)
                .build();

        // The save is cascaded from the parent attribute, but saving explicitly is also
        // fine.
        attribute.getAttributeValues().add(newAttributeValue);

        return AttributeDto.toDto(attributeRepository.save(attribute));
    }

    /**
     * Deletes an attribute. This should be used with caution.
     * In a real application, you'd add checks to see if it's in use by any product
     * variants.
     *
     * @param attributeId The ID of the attribute to delete.
     */
    @Transactional
    public void deleteAttribute(Long attributeId) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new ResourceNotFoundException("Attribute", "id", attributeId);
        }
        // TODO: Add validation to prevent deletion if the attribute is in use by
        // variants.
        attributeRepository.deleteById(attributeId);
    }

    /**
     * Deletes a specific value from an attribute.
     *
     * @param valueId The ID of the attribute value to delete.
     */
    @Transactional
    public void deleteAttributeValue(Long valueId) {
        if (!attributeValueRepository.existsById(valueId)) {
            throw new ResourceNotFoundException("AttributeValue", "id", valueId);
        }
        // TODO: Add validation to prevent deletion if the value is in use by variants.
        attributeValueRepository.deleteById(valueId);
    }

    /**
     * Retrieves all attributes along with their values.
     *
     * @return A list of all attributes as DTOs.
     */
    @Transactional(readOnly = true)
    public List<AttributeDto> findAllAttributes() {
        return attributeRepository.findAll().stream()
                .map(AttributeDto::toDto)
                .collect(Collectors.toList());
    }
}