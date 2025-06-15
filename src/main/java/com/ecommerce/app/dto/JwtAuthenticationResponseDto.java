package com.ecommerce.app.dto;
// Response after successful login
public record JwtAuthenticationResponseDto(String accessToken, UserDto user) {}
