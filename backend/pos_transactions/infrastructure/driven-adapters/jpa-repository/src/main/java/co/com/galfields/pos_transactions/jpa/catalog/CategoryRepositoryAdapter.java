package co.com.galfields.pos_transactions.jpa.catalog;

import co.com.galfields.pos_transactions.model.catalog.Category;
import co.com.galfields.pos_transactions.model.catalog.gateways.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository repository;

    @Override
    public Optional<Category> findById(Long categoryId) {
        return repository.findById(categoryId).map(this::toDomain);
    }

    @Override
    public List<Category> findAllOrderByName() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = new CategoryEntity();
        entity.setCategoryId(category.getCategoryId());
        entity.setName(category.getName());
        entity.setDescription(category.getDescription());
        // createdAt is @CreationTimestamp/updatable=false: harmless to set
        // on insert (Hibernate always generates a fresh value there
        // regardless), but required on update — a freshly-constructed
        // entity has no way to know the original value otherwise, and
        // merge() would otherwise null it out in the returned object (the
        // DB row itself stays correct since the column is excluded from
        // the UPDATE statement either way).
        entity.setCreatedAt(category.getCreatedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long categoryId) {
        repository.deleteById(categoryId);
    }

    private Category toDomain(CategoryEntity entity) {
        return Category.builder()
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
