package com.ecommerce.app.dto;
import java.util.Set;
// For an admin updating any user
public record AdminUserUpdateDto(String firstName, String lastName, String phoneNumber, boolean isActive, Set<String> roles) {}
