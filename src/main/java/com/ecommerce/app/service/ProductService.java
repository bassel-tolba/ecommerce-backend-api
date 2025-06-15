// --- FILE: ProductService.java (Refactored) ---
package com.ecommerce.app.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ecommerce.app.dto.ProductCreateDto;
import com.ecommerce.app.dto.ProductDto;
import com.ecommerce.app.dto.ProductUpdateDto;
import com.ecommerce.app.dto.ProductVariantCreateDto;
import com.ecommerce.app.dto.ProductVariantUpdateDto;
import com.ecommerce.app.exception.DuplicateResourceException;
import com.ecommerce.app.exception.InvalidOperationException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.AttributeValue;
import com.ecommerce.app.model.Category;
import com.ecommerce.app.model.Product;
import com.ecommerce.app.model.ProductVariant;
import com.ecommerce.app.repository.AttributeValueRepository;
import com.ecommerce.app.repository.CategoryRepository;
import com.ecommerce.app.repository.ProductRepository;
import com.ecommerce.app.repository.ProductVariantRepository;
import com.github.slugify.Slugify;

import lombok.RequiredArgsConstructor;

/**
 * Service class for managing products and their variants.
 *
 * Provides a complete set of CRUD operations with a focus on non-destructive,
 * granular updates for product variants and their attributes.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final Slugify slugify = Slugify.builder().build();

    /**
     * Creates a new product with its initial set of variants.
     *
     * @param createDto DTO containing product and variant creation data.
     * @return A ProductDto representing the newly created product.
     */
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

        // Save the product first to get an ID for variant association.
        Product savedProduct = productRepository.save(product);

        Set<ProductVariant> variants = createDto.variants().stream()
                .map(variantDto -> createVariantEntity(variantDto, savedProduct))
                .collect(Collectors.toSet());

        savedProduct.getVariants().addAll(variants);

        return ProductDto.toDto(productRepository.save(savedProduct));
    }

    /**
     * Performs a granular update on a product and its variants.
     * This method can create, update, and delete variants in a single transaction.
     *
     * @param productId The ID of the product to update.
     * @param updateDto DTO containing the comprehensive update instructions.
     * @return A ProductDto of the updated product.
     */
    @Transactional
    public ProductDto updateProduct(Long productId, ProductUpdateDto updateDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Step 1: Update top-level product fields
        product.setName(updateDto.name());
        product.setSlug(slugify.slugify(updateDto.name()));
        product.setDescription(updateDto.description());
        product.setBasePrice(updateDto.basePrice());
        if (!CollectionUtils.isEmpty(updateDto.categoryIds())) {
            product.setCategories(new HashSet<>(categoryRepository.findAllById(updateDto.categoryIds())));
        }

        // Step 2: Handle Variant Deletions
        if (!CollectionUtils.isEmpty(updateDto.variantIdsToDelete())) {
            Set<ProductVariant> variantsToDelete = product.getVariants().stream()
                    .filter(v -> updateDto.variantIdsToDelete().contains(v.getId()))
                    .collect(Collectors.toSet());
            product.getVariants().removeAll(variantsToDelete);
        }

        // Step 3: Handle Variant Updates
        if (!CollectionUtils.isEmpty(updateDto.variantsToUpdate())) {
            updateDto.variantsToUpdate().forEach(variantUpdateDto -> {
                ProductVariant variant = product.getVariants().stream()
                        .filter(v -> v.getId().equals(variantUpdateDto.id()))
                        .findFirst()
                        .orElseThrow(() -> new InvalidOperationException(
                                "Variant with ID " + variantUpdateDto.id() + " does not belong to this product."));

                updateVariantEntity(variant, variantUpdateDto);
            });
        }

        // Step 4: Handle Variant Creations
        if (!CollectionUtils.isEmpty(updateDto.variantsToCreate())) {
            updateDto.variantsToCreate().forEach(variantCreateDto -> {
                if (productVariantRepository.existsBySku(variantCreateDto.sku())) {
                    throw new DuplicateResourceException("ProductVariant", "sku", variantCreateDto.sku());
                }
                ProductVariant newVariant = createVariantEntity(variantCreateDto, product);
                product.getVariants().add(newVariant);
            });
        }

        Product savedProduct = productRepository.save(product);
        return ProductDto.toDto(savedProduct);
    }

    // --- Helper for creating a new variant entity ---
    private ProductVariant createVariantEntity(ProductVariantCreateDto dto, Product product) {
        Set<AttributeValue> attributeValues = resolveAttributeValues(dto.attributeValueIds());
        return ProductVariant.builder()
                .product(product)
                .sku(dto.sku())
                .price(dto.price())
                .isActive(true)
                .attributeValues(attributeValues)
                .build();
    }

    // --- Helper for updating an existing variant entity ---
    private void updateVariantEntity(ProductVariant variant, ProductVariantUpdateDto dto) {
        if (dto.sku() != null)
            variant.setSku(dto.sku());
        if (dto.price() != null)
            variant.setPrice(dto.price());
        if (dto.isActive() != null)
            variant.setActive(dto.isActive());
        if (!CollectionUtils.isEmpty(dto.attributeValueIds())) {
            variant.setAttributeValues(resolveAttributeValues(dto.attributeValueIds()));
        }
    }

    // --- Helper for resolving AttributeValue IDs ---
    private Set<AttributeValue> resolveAttributeValues(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new HashSet<>();
        }
        List<AttributeValue> foundValues = attributeValueRepository.findByIdIn(ids);
        if (foundValues.size() != ids.size()) {
            Set<Long> foundIds = foundValues.stream().map(AttributeValue::getId).collect(Collectors.toSet());
            ids.removeAll(foundIds);
            throw new ResourceNotFoundException("AttributeValue", "ids", ids.toString());
        }
        return new HashSet<>(foundValues);
    }

    // --- Other existing methods (findAll, findBySlug, etc.) remain largely the
    // same ---

    @Transactional(readOnly = true)
    public Page<ProductDto> findAllProducts(Pageable pageable, boolean publishedOnly) {
        Page<Product> productPage = publishedOnly
                ? productRepository.findByIsPublishedTrue(pageable)
                : productRepository.findAll(pageable);
        return productPage.map(ProductDto::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto findProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(ProductDto::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
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