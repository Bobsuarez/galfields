package co.com.galfields.pos_transactions.model.catalog.gateways;

import co.com.galfields.pos_transactions.model.catalog.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findById(Long categoryId);

    List<Category> findAllOrderByName();

    Category save(Category category);

    void deleteById(Long categoryId);
}
