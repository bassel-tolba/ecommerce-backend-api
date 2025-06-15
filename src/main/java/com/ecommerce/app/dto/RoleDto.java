package com.ecommerce.app.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.ecommerce.app.model.Role;

// FIX: Removed @Builder from record.
public record RoleDto(
        Long id,
        String name,
        String description,
        Set<String> permissions) {
    public static RoleDto toDto(Role role) {
        if (role == null) {
            return null;
        }
        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream().map(p -> p.getName()).collect(Collectors.toSet()));
    }

    public Role toEntity() {
        Role role = new Role();
        role.setId(this.id);
        role.setName(this.name);
        role.setDescription(this.description);
        return role;
    }
}
