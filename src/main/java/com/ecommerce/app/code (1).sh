#!/bin/bash

# ==============================================================================
# E-Commerce Professional Application Scaffolding Script (V2 - Corrected)
# ==============================================================================
# FIXES:
# 1. Added @Builder.Default to all initialized fields in Models to resolve
#    Lombok builder warnings.
# 2. Removed @Builder from all DTO records to fix compilation errors. Records
#    serve as complete DTOs without needing a builder in this context.
# ==============================================================================

echo "Starting scaffolding process (V2)..."

# --- Verify correct directory structure ---
DIRS=("model" "repository" "dto" "exception" "config" "service" "controller" "util")
for DIR in "${DIRS[@]}"; do
    if [ ! -d "$DIR" ]; then
        echo "Error: Directory '$DIR' not found. Please run this script from your base package directory."
        exit 1
    fi
done

# ==============================================================================
# 1. Base Classes & Enums
# ==============================================================================

echo "Generating Base and Auditable Entities..."

# --- model/BaseEntity.java ---
cat <<'EOF' > model/BaseEntity.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }
}
EOF

# --- model/AuditableEntity.java ---
cat <<'EOF' > model/AuditableEntity.java
package com.ecommerce.app.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@MappedSuperclass
@Audited
public abstract class AuditableEntity extends BaseEntity {
}
EOF

echo "Generating Enums..."

# --- model/OrderStatus.java ---
cat <<'EOF' > model/OrderStatus.java
package com.ecommerce.app.model;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
EOF

# ==============================================================================
# 2. Exception Handling
# ==============================================================================

echo "Generating Exception Handling classes..."

# --- exception/ResourceNotFoundException.java ---
cat <<'EOF' > exception/ResourceNotFoundException.java
package com.ecommerce.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
EOF

# --- exception/GlobalExceptionHandler.java ---
cat <<'EOF' > exception/GlobalExceptionHandler.java
package com.ecommerce.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(
            Exception ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact support.");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
EOF


# ==============================================================================
# 3. User & RBAC (Role-Based Access Control)
# ==============================================================================

echo "Generating User and RBAC components..."

# --- MODEL ---
cat <<'EOF' > model/User.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "phone_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class User extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone_number")
    private String phoneNumber;

    // FIX: Added @Builder.Default to respect initialization
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private Set<Address> addresses = new HashSet<>();
}
EOF

cat <<'EOF' > model/Role.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @ToString.Exclude
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private Set<Permission> permissions = new HashSet<>();
}
EOF

cat <<'EOF' > model/Permission.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name; // e.g., "product:create", "order:view_all"

    private String description;
}
EOF

# --- REPOSITORY ---
cat <<'EOF' > repository/UserRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, RevisionRepository<User, Long, Integer> {
    Optional<User> findByEmail(String email);
}
EOF

cat <<'EOF' > repository/RoleRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
EOF

cat <<'EOF' > repository/PermissionRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
}
EOF

# --- DTO ---
cat <<'EOF' > dto/UserDto.java
package com.ecommerce.app.dto;

import com.ecommerce.app.model.User;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

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
        Instant updatedAt
) {
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
                user.getUpdatedAt()
        );
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
EOF

cat <<'EOF' > dto/RoleDto.java
package com.ecommerce.app.dto;

import com.ecommerce.app.model.Role;
import java.util.Set;
import java.util.stream.Collectors;

// FIX: Removed @Builder from record.
public record RoleDto(
    Long id,
    String name,
    String description,
    Set<String> permissions
) {
    public static RoleDto toDto(Role role) {
        if (role == null) {
            return null;
        }
        return new RoleDto(
            role.getId(),
            role.getName(),
            role.getDescription(),
            role.getPermissions().stream().map(p -> p.getName()).collect(Collectors.toSet())
        );
    }

    public Role toEntity() {
        Role role = new Role();
        role.setId(this.id);
        role.setName(this.name);
        role.setDescription(this.description);
        return role;
    }
}
EOF

# ==============================================================================
# 4. Product Catalog
# ==============================================================================

echo "Generating Product Catalog components..."

# --- MODEL ---
cat <<'EOF' > model/Product.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @Column(name = "is_published", nullable = false)
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private boolean isPublished = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private Set<ProductVariant> variants = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_categories",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @ToString.Exclude
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private Set<Category> categories = new HashSet<>();
}
EOF

cat <<'EOF' > model/ProductVariant.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product_variants", uniqueConstraints = {
    @UniqueConstraint(columnNames = "sku")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String sku;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "weight_kg", precision = 6, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "is_active", nullable = false)
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "variant_attribute_values",
        joinColumns = @JoinColumn(name = "variant_id"),
        inverseJoinColumns = @JoinColumn(name = "attribute_value_id")
    )
    @ToString.Exclude
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private Set<AttributeValue> attributeValues = new HashSet<>();
}
EOF

cat <<'EOF' > model/Attribute.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribute extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String name; // e.g., "Color", "Size"
}
EOF

cat <<'EOF' > model/AttributeValue.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attribute_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeValue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @Column(nullable = false)
    private String value; // e.g., "Red", "Large"
}
EOF

cat <<'EOF' > model/Category.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;
}
EOF

# --- REPOSITORY ---
cat <<'EOF' > repository/ProductRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, RevisionRepository<Product, Long, Integer> {
    Optional<Product> findBySlug(String slug);
}
EOF

cat <<'EOF' > repository/ProductVariantRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, RevisionRepository<ProductVariant, Long, Integer> {
    Optional<ProductVariant> findBySku(String sku);
}
EOF

cat <<'EOF' > repository/CategoryRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
}
EOF

# --- DTO ---
cat <<'EOF' > dto/ProductDto.java
package com.ecommerce.app.dto;

import com.ecommerce.app.model.Product;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

// FIX: Removed @Builder from record.
public record ProductDto(
    Long id,
    String name,
    String slug,
    String description,
    BigDecimal basePrice,
    boolean isPublished,
    Set<ProductVariantDto> variants,
    Set<String> categories
) {
    public static ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDto(
            product.getId(),
            product.getName(),
            product.getSlug(),
            product.getDescription(),
            product.getBasePrice(),
            product.isPublished(),
            product.getVariants().stream().map(ProductVariantDto::toDto).collect(Collectors.toSet()),
            product.getCategories().stream().map(c -> c.getName()).collect(Collectors.toSet())
        );
    }
    
    public Product toEntity() {
        Product p = new Product();
        p.setId(this.id);
        p.setName(this.name);
        p.setSlug(this.slug);
        p.setDescription(this.description);
        p.setBasePrice(this.basePrice);
        p.setPublished(this.isPublished);
        return p;
    }
}
EOF

cat <<'EOF' > dto/ProductVariantDto.java
package com.ecommerce.app.dto;

import com.ecommerce.app.model.ProductVariant;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

// FIX: Removed @Builder from record.
public record ProductVariantDto(
    Long id,
    Long productId,
    String sku,
    BigDecimal price,
    BigDecimal weightKg,
    boolean isActive,
    Map<String, String> attributes
) {
    public static ProductVariantDto toDto(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        return new ProductVariantDto(
            variant.getId(),
            variant.getProduct().getId(),
            variant.getSku(),
            variant.getPrice(),
            variant.getWeightKg(),
            variant.isActive(),
            variant.getAttributeValues().stream()
                .collect(Collectors.toMap(
                    av -> av.getAttribute().getName(),
                    av -> av.getValue()
                ))
        );
    }
    
    public ProductVariant toEntity() {
        ProductVariant pv = new ProductVariant();
        pv.setId(this.id);
        pv.setSku(this.sku);
        pv.setPrice(this.price);
        pv.setWeightKg(this.weightKg);
        pv.setActive(this.isActive);
        return pv;
    }
}
EOF

# ==============================================================================
# 5. Order & Fulfillment
# ==============================================================================

echo "Generating Order and Fulfillment components..."

# --- MODEL ---
cat <<'EOF' > model/Order.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends AuditableEntity {

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingCost;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxes;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;

    @ManyToOne
    @JoinColumn(name = "billing_address_id", nullable = false)
    private Address billingAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private Set<OrderItem> orderItems = new HashSet<>();
}
EOF

cat <<'EOF' > model/OrderItem.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_per_unit", precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePerUnit;
}
EOF

cat <<'EOF' > model/Address.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "address_line_1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(name = "state_province", nullable = false)
    private String stateProvince;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String country;

    @Column(name = "is_default_shipping")
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private boolean isDefaultShipping = false;

    @Column(name = "is_default_billing")
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private boolean isDefaultBilling = false;
}
EOF

# --- REPOSITORY ---
cat <<'EOF' > repository/OrderRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, RevisionRepository<Order, Long, Integer> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserId(Long userId);
}
EOF

cat <<'EOF' > repository/AddressRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
}
EOF

# --- DTO ---
cat <<'EOF' > dto/OrderDto.java
package com.ecommerce.app.dto;

import com.ecommerce.app.model.Order;
import com.ecommerce.app.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

// FIX: Removed @Builder from record.
public record OrderDto(
    Long id,
    String orderNumber,
    Long userId,
    OrderStatus status,
    BigDecimal subtotal,
    BigDecimal shippingCost,
    BigDecimal taxes,
    BigDecimal totalAmount,
    AddressDto shippingAddress,
    AddressDto billingAddress,
    String notes,
    Instant createdAt,
    Set<OrderItemDto> items
) {
    public static OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderDto(
            order.getId(),
            order.getOrderNumber(),
            order.getUser().getId(),
            order.getStatus(),
            order.getSubtotal(),
            order.getShippingCost(),
            order.getTaxes(),
            order.getTotalAmount(),
            AddressDto.toDto(order.getShippingAddress()),
            AddressDto.toDto(order.getBillingAddress()),
            order.getNotes(),
            order.getCreatedAt(),
            order.getOrderItems().stream().map(OrderItemDto::toDto).collect(Collectors.toSet())
        );
    }
}
EOF

cat <<'EOF' > dto/OrderItemDto.java
package com.ecommerce.app.dto;

import com.ecommerce.app.model.OrderItem;
import java.math.BigDecimal;

// FIX: Removed @Builder from record.
public record OrderItemDto(
    Long id,
    String variantSku,
    String productName,
    Integer quantity,
    BigDecimal pricePerUnit
) {
    public static OrderItemDto toDto(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemDto(
            item.getId(),
            item.getVariant().getSku(),
            item.getVariant().getProduct().getName(),
            item.getQuantity(),
            item.getPricePerUnit()
        );
    }
}
EOF

cat <<'EOF' > dto/AddressDto.java
package com.ecommerce.app.dto;

import com.ecommerce.app.model.Address;

// FIX: Removed @Builder from record.
public record AddressDto(
    Long id,
    String addressLine1,
    String addressLine2,
    String city,
    String stateProvince,
    String postalCode,
    String country
) {
    public static AddressDto toDto(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDto(
            address.getId(),
            address.getAddressLine1(),
            address.getAddressLine2(),
            address.getCity(),
            address.getStateProvince(),
            address.getPostalCode(),
            address.getCountry()
        );
    }

    public Address toEntity() {
        Address address = new Address();
        address.setId(this.id);
        address.setAddressLine1(this.addressLine1);
        address.setAddressLine2(this.addressLine2);
        address.setCity(this.city);
        address.setStateProvince(this.stateProvince);
        address.setPostalCode(this.postalCode);
        address.setCountry(this.country);
        return address;
    }
}
EOF

# ==============================================================================
# 6. Advanced Inventory
# ==============================================================================

echo "Generating Advanced Inventory components..."

# --- MODEL ---
cat <<'EOF' > model/Warehouse.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
}
EOF

cat <<'EOF' > model/InventoryStockId.java
package com.ecommerce.app.model;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InventoryStockId implements Serializable {
    private Long variantId;
    private Long warehouseId;
}
EOF

cat <<'EOF' > model/InventoryStock.java
package com.ecommerce.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryStock {

    @EmbeddedId
    private InventoryStockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("variantId")
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("warehouseId")
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default // FIX: Added @Builder.Default to respect initialization
    private Integer reservedQuantity = 0;
}
EOF

# --- REPOSITORY ---
cat <<'EOF' > repository/WarehouseRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}
EOF

cat <<'EOF' > repository/InventoryStockRepository.java
package com.ecommerce.app.repository;

import com.ecommerce.app.model.InventoryStock;
import com.ecommerce.app.model.InventoryStockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, InventoryStockId> {
}
EOF

# --- DTO ---
cat <<'EOF' > dto/InventoryStockDto.java
package com.ecommerce.app.dto;

import com.ecommerce.app.model.InventoryStock;

// FIX: Removed @Builder from record.
public record InventoryStockDto(
    Long variantId,
    String variantSku,
    Long warehouseId,
    String warehouseName,
    Integer quantity,
    Integer reservedQuantity,
    Integer availableQuantity
) {
    public static InventoryStockDto toDto(InventoryStock stock) {
        if (stock == null) {
            return null;
        }
        return new InventoryStockDto(
            stock.getId().getVariantId(),
            stock.getVariant().getSku(),
            stock.getId().getWarehouseId(),
            stock.getWarehouse().getName(),
            stock.getQuantity(),
            stock.getReservedQuantity(),
            stock.getQuantity() - stock.getReservedQuantity()
        );
    }
}
EOF

# ==============================================================================
# 7. Finalizing AppApplication
# ==============================================================================

echo "Finalizing AppApplication.java to enable auditing..."

cat <<'EOF' > AppApplication.java
package com.ecommerce.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

}
EOF

echo "=================================================="
echo "Scaffolding complete! All fixes have been applied."
echo "Your project should now compile without errors."
echo "=================================================="