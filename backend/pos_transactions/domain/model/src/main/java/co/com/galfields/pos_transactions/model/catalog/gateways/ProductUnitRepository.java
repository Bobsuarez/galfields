package co.com.galfields.pos_transactions.model.catalog.gateways;

import co.com.galfields.pos_transactions.model.catalog.ProductUnit;

import java.util.Optional;

public interface ProductUnitRepository {
    Optional<ProductUnit> findByIdAndVariantId(Long productUnitId, Long variantId);

    ProductUnit save(ProductUnit unit);
}
