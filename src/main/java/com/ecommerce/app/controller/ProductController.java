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
