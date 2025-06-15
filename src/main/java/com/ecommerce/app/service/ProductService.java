package com.ecommerce.app.service;

import com.ecommerce.app.dto.*;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.*;
import com.ecommerce.app.repository.*;
import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final Slugify slugify = Slugify.builder().build();

    @Transactional
    public ProductDto createProduct(ProductCreateDto createDto) {
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(createDto.categoryIds()));

        Product product = Product.builder()
                .name(createDto.name())
                .slug(slugify.slugify(createDto.name()))
                .description(createDto.description())
                .basePrice(createDto.basePrice())
                .isPublished(false) // Products are created as drafts
                .categories(categories)
                .build();
        
        Product savedProduct = productRepository.save(product);

        Set<ProductVariant> variants = createDto.variants().stream().map(variantDto -> 
            ProductVariant.builder()
                .product(savedProduct)
                .sku(variantDto.sku())
                .price(variantDto.price())
                .isActive(true)
                .build()
        ).collect(Collectors.toSet());

        productVariantRepository.saveAll(variants);
        savedProduct.setVariants(variants);

        return ProductDto.toDto(savedProduct);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductDto> findAllProducts(Pageable pageable, boolean publishedOnly) {
        Page<Product> productPage;
        if (publishedOnly) {
             productPage = productRepository.findByIsPublishedTrue(pageable);
        } else {
             productPage = productRepository.findAll(pageable);
        }
        return productPage.map(ProductDto::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto findProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(ProductDto::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
    }
    
    @Transactional
    public ProductDto updateProduct(Long productId, ProductCreateDto updateDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(updateDto.categoryIds()));

        product.setName(updateDto.name());
        product.setSlug(slugify.slugify(updateDto.name()));
        product.setDescription(updateDto.description());
        product.setBasePrice(updateDto.basePrice());
        product.setCategories(categories);

        // NOTE: More complex variant update logic would be needed for a real app
        // This simple implementation replaces all variants
        product.getVariants().clear();
        Set<ProductVariant> variants = updateDto.variants().stream().map(variantDto ->
            ProductVariant.builder()
                .product(product)
                .sku(variantDto.sku())
                .price(variantDto.price())
                .isActive(true)
                .build()
        ).collect(Collectors.toSet());
        product.getVariants().addAll(variants);
        
        Product savedProduct = productRepository.save(product);
        return ProductDto.toDto(savedProduct);
    }
    
    @Transactional
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        productRepository.deleteById(productId);
    }

    @Transactional
    public ProductDto setProductPublicationStatus(Long productId, boolean isPublished) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        product.setPublished(isPublished);
        return ProductDto.toDto(productRepository.save(product));
    }
}
