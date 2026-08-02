package co.com.galfields.pos_transactions.api.catalog;

import co.com.galfields.pos_transactions.model.catalog.Category;
import co.com.galfields.pos_transactions.usecase.catalog.CategoryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Mirrors backend/pos's CategoryController 1:1. */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CategoryResponse create(@RequestBody @Valid CategoryRequest request) {
        return toResponse(categoryUseCase.create(toDomain(request)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryUseCase.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{categoryId}")
    @Transactional(readOnly = true)
    public CategoryResponse get(@PathVariable("categoryId") Long categoryId) {
        return toResponse(categoryUseCase.get(categoryId));
    }

    @PutMapping("/{categoryId}")
    @Transactional
    public CategoryResponse update(@PathVariable("categoryId") Long categoryId, @RequestBody @Valid CategoryRequest request) {
        return toResponse(categoryUseCase.update(categoryId, toDomain(request)));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("categoryId") Long categoryId) {
        categoryUseCase.delete(categoryId);
    }

    private Category toDomain(CategoryRequest request) {
        return Category.builder().name(request.name()).description(request.description()).build();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getCategoryId(), category.getName(), category.getDescription(), category.getCreatedAt());
    }
}
