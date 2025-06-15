package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, RevisionRepository<User, Long, Integer> {
    Optional<User> findByEmail(String email);
}
