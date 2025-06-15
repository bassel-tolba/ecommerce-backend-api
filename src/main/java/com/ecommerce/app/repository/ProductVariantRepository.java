package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.ProductVariant;

@Repository
public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long>, RevisionRepository<ProductVariant, Long, Integer> {
    Optional<ProductVariant> findBySku(String sku);
}
