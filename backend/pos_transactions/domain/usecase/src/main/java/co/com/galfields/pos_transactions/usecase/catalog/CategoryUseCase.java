package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.Category;
import co.com.galfields.pos_transactions.model.catalog.gateways.CategoryRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Mirrors backend/pos's CategoryService — plain CRUD, no soft-delete
 * (categories has no is_active column), no app-level name-uniqueness check
 * (none in the DB either, duplicates allowed on purpose). */
@RequiredArgsConstructor
public class CategoryUseCase {

    private final CategoryRepository categoryRepository;

    public Category create(Category category) {
        return categoryRepository.save(category);
    }

    public Category get(Long categoryId) {
        return findOrThrow(categoryId);
    }

    public List<Category> list() {
        return categoryRepository.findAllOrderByName();
    }

    public Category update(Long categoryId, Category patch) {
        Category existing = findOrThrow(categoryId);
        existing.setName(patch.getName());
        existing.setDescription(patch.getDescription());
        return categoryRepository.save(existing);
    }

    public void delete(Long categoryId) {
        findOrThrow(categoryId);
        categoryRepository.deleteById(categoryId);
    }

    private Category findOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + categoryId + " not found"));
    }
}
