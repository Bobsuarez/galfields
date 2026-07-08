package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.CategoryRequest;
import co.com.galfields.pos.dto.CategoryResponse;
import co.com.galfields.pos.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@RequestBody @Valid CategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.listCategories();
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse get(@PathVariable Long categoryId) {
        return categoryService.getCategory(categoryId);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse update(@PathVariable Long categoryId, @RequestBody @Valid CategoryRequest request) {
        return categoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
    }
}
