package com.ecommerce.app.controller;

import com.ecommerce.app.dto.JwtAuthenticationResponseDto;
import com.ecommerce.app.dto.LoginRequestDto;
import com.ecommerce.app.dto.UserDto;
import com.ecommerce.app.dto.UserRegistrationDto;
import com.ecommerce.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponseDto> authenticateUser(@RequestBody LoginRequestDto loginRequest) {
        JwtAuthenticationResponseDto response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserRegistrationDto registrationDto) {
        UserDto registeredUser = authService.registerUser(registrationDto);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }
}
