package com.ecommerce.app.controller;

import com.ecommerce.app.dto.AdminUserUpdateDto;
import com.ecommerce.app.dto.UserDto;
import com.ecommerce.app.dto.UserProfileUpdateDto;
import com.ecommerce.app.security.UserPrincipal;
import com.ecommerce.app.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Endpoint for a logged-in user to get their own profile
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUserProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserDto userDto = userService.findUserById(currentUser.getId());
        return ResponseEntity.ok(userDto);
    }

    // Endpoint for a logged-in user to update their own profile
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateCurrentUserProfile(
            @RequestBody UserProfileUpdateDto updateDto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserDto updatedUser = userService.updateUserProfile(currentUser.getId(), updateDto, currentUser);
        return ResponseEntity.ok(updatedUser);
    }

    // --- ADMIN-ONLY ENDPOINTS ---

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(@Parameter(hidden = true) Pageable pageable) {
        Page<UserDto> users = userService.findAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto userDto = userService.findUserById(id);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserByAdmin(@PathVariable Long id, @RequestBody AdminUserUpdateDto updateDto) {
        UserDto updatedUser = userService.adminUpdateUser(id, updateDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
