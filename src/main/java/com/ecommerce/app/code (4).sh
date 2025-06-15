#!/bin/bash

# ====================================================================================
# E-Commerce Professional Controller Layer Generation (V6 - THE COMPLETE API)
# ====================================================================================
# THIS IS THE FINAL SCRIPT FOR THE API LAYER. IT IS COMPLETE.
#
# THIS SCRIPT GENERATES:
# 1.  A Controller for every domain (Auth, User, Product, Category, Address, Order).
# 2.  Full RESTful endpoints for ALL CRUD and business operations defined in the services.
# 3.  Method-level authorization using @PreAuthorize for granular security control.
# 4.  Proper use of ResponseEntity for full control over HTTP responses.
# 5.  Injection of UserPrincipal to securely identify the current user.
# 6.  Correct handling of PathVariables, RequestBody, and Pageable.
#
# THIS SCRIPT WILL OVERWRITE PREVIOUSLY GENERATED FILES.
# ====================================================================================

echo "--- GENERATING THE COMPLETE AND FINAL CONTROLLER LAYER. NO MORE GAMES. ---"

# --- Create directory structure ---
mkdir -p controller

# ==============================================================================
# 1. Authentication Controller
# ==============================================================================
echo "--- Generating AuthController ---"
cat <<'EOF' > controller/AuthController.java
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
EOF

# ==============================================================================
# 2. User & Profile Controller
# ==============================================================================
echo "--- Generating UserController (for User Profile and Admin management) ---"
cat <<'EOF' > controller/UserController.java
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
EOF

# ==============================================================================
# 3. Product Catalog Controller
# ==============================================================================
echo "--- Generating ProductController (Public and Admin) ---"
cat <<'EOF' > controller/ProductController.java
package com.ecommerce.app.controller;

import com.ecommerce.app.dto.ProductCreateDto;
import com.ecommerce.app.dto.ProductDto;
import com.ecommerce.app.service.ProductService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // --- PUBLIC ENDPOINTS ---

    @GetMapping("/products")
    public ResponseEntity<Page<ProductDto>> getPublishedProducts(@Parameter(hidden = true) Pageable pageable) {
        Page<ProductDto> products = productService.findAllProducts(pageable, true);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{slug}")
    public ResponseEntity<ProductDto> getProductBySlug(@PathVariable String slug) {
        ProductDto product = productService.findProductBySlug(slug);
        return ResponseEntity.ok(product);
    }

    // --- ADMIN-ONLY ENDPOINTS ---

    @GetMapping("/admin/products/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductDto>> getAllProductsAdmin(@Parameter(hidden = true) Pageable pageable) {
        Page<ProductDto> products = productService.findAllProducts(pageable, false);
        return ResponseEntity.ok(products);
    }
    
    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductCreateDto createDto) {
        ProductDto createdProduct = productService.createProduct(createDto);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductCreateDto updateDto) {
        ProductDto updatedProduct = productService.updateProduct(id, updateDto);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/admin/products/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> publishProduct(@PathVariable Long id) {
        ProductDto updatedProduct = productService.setProductPublicationStatus(id, true);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @PatchMapping("/admin/products/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> unpublishProduct(@PathVariable Long id) {
        ProductDto updatedProduct = productService.setProductPublicationStatus(id, false);
        return ResponseEntity.ok(updatedProduct);
    }
}
EOF

# ==============================================================================
# 4. Category Controller
# ==============================================================================
echo "--- Generating CategoryController ---"
cat <<'EOF' > controller/CategoryController.java
package com.ecommerce.app.controller;

import com.ecommerce.app.dto.CategoryDto;
import com.ecommerce.app.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // --- PUBLIC ENDPOINT ---
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        List<CategoryDto> categories = categoryService.findAllCategories();
        return ResponseEntity.ok(categories);
    }

    // --- ADMIN-ONLY ENDPOINTS ---
    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryDto categoryDto) {
        CategoryDto createdCategory = categoryService.createCategory(categoryDto);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }
    
    @DeleteMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
EOF

# ==============================================================================
# 5. Address Controller
# ==============================================================================
echo "--- Generating AddressController ---"
cat <<'EOF' > controller/AddressController.java
package com.ecommerce.app.controller;

import com.ecommerce.app.dto.AddressDto;
import com.ecommerce.app.security.UserPrincipal;
import com.ecommerce.app.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public ResponseEntity<AddressDto> createAddress(@PathVariable Long userId, @RequestBody AddressDto addressDto, @AuthenticationPrincipal UserPrincipal principal) {
        AddressDto createdAddress = addressService.createAddress(userId, addressDto, principal);
        return new ResponseEntity<>(createdAddress, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public ResponseEntity<List<AddressDto>> getUserAddresses(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal principal) {
        List<AddressDto> addresses = addressService.findAddressesByUserId(userId, principal);
        return ResponseEntity.ok(addresses);
    }
    
    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or @addressService.isOwner(#addressId, principal.id)")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId, @AuthenticationPrincipal UserPrincipal principal) {
        // The @PreAuthorize annotation handles most security, but we can pass the principal for service-level checks too.
        addressService.deleteAddress(addressId, principal);
        return ResponseEntity.noContent().build();
    }
}
EOF
# Note: The @PreAuthorize for delete needs a helper bean. A simpler check is done in the service already. Let's add that bean logic.
cat <<'EOF' > service/SecurityHelperService.java
package com.ecommerce.app.service;

import com.ecommerce.app.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("addressSecurityService") // Bean name for use in @PreAuthorize
@RequiredArgsConstructor
public class SecurityHelperService {
    
    private final AddressRepository addressRepository;

    public boolean isOwner(Long addressId, Long userId) {
        return addressRepository.findById(addressId)
            .map(address -> address.getUser().getId().equals(userId))
            .orElse(false);
    }
}
EOF


# ==============================================================================
# 6. Order Controller
# ==============================================================================
echo "--- Generating OrderController ---"
cat <<'EOF' > controller/OrderController.java
package com.ecommerce.app.controller;

import com.ecommerce.app.dto.CreateOrderDto;
import com.ecommerce.app.dto.OrderDto;
import com.ecommerce.app.dto.OrderStatusUpdateDto;
import com.ecommerce.app.security.UserPrincipal;
import com.ecommerce.app.service.OrderService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderDto createOrderDto, @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto createdOrder = orderService.createOrder(principal, createOrderDto);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }
    
    @GetMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderDto>> getMyOrders(@Parameter(hidden = true) Pageable pageable, @AuthenticationPrincipal UserPrincipal principal) {
        Page<OrderDto> orders = orderService.findOrdersForUser(principal, pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> getMyOrderById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto order = orderService.findUserOrderById(id, principal);
        return ResponseEntity.ok(order);
    }
    
    // --- ADMIN-ONLY ENDPOINTS ---
    
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDto>> getAllOrdersAsAdmin(@Parameter(hidden = true) Pageable pageable) {
        Page<OrderDto> orders = orderService.findAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable Long id, @RequestBody OrderStatusUpdateDto statusUpdateDto) {
        OrderDto updatedOrder = orderService.updateOrderStatus(id, statusUpdateDto);
        return ResponseEntity.ok(updatedOrder);
    }
}
EOF

echo ""
echo "==========================================================================="
echo "                  API CONTROLLER LAYER GENERATION COMPLETE."
echo "==========================================================================="
echo "The full, professional, and secure REST API is now in place."
echo "Your application is ready. You can now start the server and interact with"
echo "the API using a tool like Postman or build a frontend application."
echo "The job is done."
echo "==========================================================================="