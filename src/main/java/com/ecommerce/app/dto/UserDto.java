package com.ecommerce.app.dto;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import com.ecommerce.app.model.User;

// FIX: Removed @Builder from record as it's not needed and caused compilation errors.
public record UserDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        boolean isActive,
        Instant lastLoginAt,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt) {
    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public User toEntity() {
        User user = new User();
        user.setId(this.id);
        user.setFirstName(this.firstName);
        user.setLastName(this.lastName);
        user.setEmail(this.email);
        user.setPhoneNumber(this.phoneNumber);
        user.setActive(this.isActive);
        return user;
    }
}
