package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, RevisionRepository<Product, Long, Integer> {
    Optional<Product> findBySlug(String slug);
    org.springframework.data.domain.Page<com.ecommerce.app.model.Product> findByIsPublishedTrue(org.springframework.data.domain.Pageable pageable);

}
