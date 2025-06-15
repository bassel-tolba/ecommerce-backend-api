package com.ecommerce.app.service;

import com.ecommerce.app.dto.*;
import com.ecommerce.app.exception.DuplicateResourceException;
import com.ecommerce.app.exception.InvalidOperationException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.Role;
import com.ecommerce.app.model.User;
import com.ecommerce.app.repository.RoleRepository;
import com.ecommerce.app.repository.UserRepository;
import com.ecommerce.app.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto createUser(UserRegistrationDto registrationDto) {
        if (userRepository.findByEmail(registrationDto.email()).isPresent()) {
            throw new DuplicateResourceException("User", "email", registrationDto.email());
        }
        
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Fatal: Default role 'ROLE_USER' not found."));

        User newUser = User.builder()
            .email(registrationDto.email())
            .firstName(registrationDto.firstName())
            .lastName(registrationDto.lastName())
            .passwordHash(passwordEncoder.encode(registrationDto.password()))
            .roles(Set.of(defaultRole))
            .isActive(true)
            .build();

        User savedUser = userRepository.save(newUser);
        return UserDto.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDto::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto findUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDto::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional
    public UserDto updateUserProfile(Long userId, UserProfileUpdateDto updateDto, UserPrincipal currentUser) {
        if (!Objects.equals(currentUser.getId(), userId)) {
            throw new InvalidOperationException("You can only update your own profile.");
        }
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        userToUpdate.setFirstName(updateDto.firstName());
        userToUpdate.setLastName(updateDto.lastName());
        userToUpdate.setPhoneNumber(updateDto.phoneNumber());

        return UserDto.toDto(userRepository.save(userToUpdate));
    }

    @Transactional
    public UserDto adminUpdateUser(Long userId, AdminUserUpdateDto updateDto) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        userToUpdate.setFirstName(updateDto.firstName());
        userToUpdate.setLastName(updateDto.lastName());
        userToUpdate.setPhoneNumber(updateDto.phoneNumber());
        userToUpdate.setActive(updateDto.isActive());
        
        if (updateDto.roles() != null && !updateDto.roles().isEmpty()) {
            Set<Role> newRoles = updateDto.roles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName)))
                .collect(Collectors.toSet());
            userToUpdate.setRoles(newRoles);
        }
        
        return UserDto.toDto(userRepository.save(userToUpdate));
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        userRepository.deleteById(userId);
    }
}
