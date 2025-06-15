package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, RevisionRepository<Order, Long, Integer> {
    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserId(Long userId, Pageable pageable);
}
