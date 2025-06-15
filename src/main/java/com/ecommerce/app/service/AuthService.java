package com.ecommerce.app.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.app.dto.JwtAuthenticationResponseDto;
import com.ecommerce.app.dto.LoginRequestDto;
import com.ecommerce.app.dto.UserDto;
import com.ecommerce.app.dto.UserRegistrationDto;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.User;
import com.ecommerce.app.repository.UserRepository;
import com.ecommerce.app.security.UserPrincipal;
import com.ecommerce.app.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Transactional(readOnly = true)
    public JwtAuthenticationResponseDto authenticateUser(LoginRequestDto loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        return new JwtAuthenticationResponseDto(jwt, UserDto.toDto(user));
    }

    public UserDto registerUser(UserRegistrationDto registrationDto) {
        return userService.createUser(registrationDto);
    }
}
