package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.CategoryRequest;
import co.com.galfields.pos.dto.CategoryResponse;
import co.com.galfields.pos.entity.Category;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = new Category();
        applyFields(category, request);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long categoryId) {
        return toResponse(findOrThrow(categoryId));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = findOrThrow(categoryId);
        applyFields(category, request);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = findOrThrow(categoryId);
        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + categoryId + " not found"));
    }

    private void applyFields(Category category, CategoryRequest request) {
        category.setName(request.name());
        category.setDescription(request.description());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt());
    }
}
