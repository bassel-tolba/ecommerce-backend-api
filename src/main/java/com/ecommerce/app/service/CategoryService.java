package com.ecommerce.app.service;

import com.ecommerce.app.dto.CategoryDto;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.Category;
import com.ecommerce.app.repository.CategoryRepository;
import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final Slugify slugify = Slugify.builder().build();

    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        Category parent = null;
        if (categoryDto.parentCategoryId() != null) {
            parent = categoryRepository.findById(categoryDto.parentCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", categoryDto.parentCategoryId()));
        }
        Category category = Category.builder()
            .name(categoryDto.name())
            .slug(slugify.slugify(categoryDto.name()))
            .parentCategory(parent)
            .build();
        return CategoryDto.toDto(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> findAllCategories() {
        return categoryRepository.findAll().stream().map(CategoryDto::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }
        // Add logic here to re-parent child categories if necessary before deleting
        categoryRepository.deleteById(categoryId);
    }
}
