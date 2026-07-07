package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.CategoryResponse;
import co.com.galfields.pos.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryResponse(category.getCategoryId(), category.getName()))
                .toList();
    }
}
