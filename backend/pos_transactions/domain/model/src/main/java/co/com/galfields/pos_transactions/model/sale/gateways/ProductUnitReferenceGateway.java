package co.com.galfields.pos_transactions.model.sale.gateways;

import co.com.galfields.pos_transactions.model.sale.ProductUnitReference;

import java.util.Optional;

public interface ProductUnitReferenceGateway {
    Optional<ProductUnitReference> find(Long productUnitId, Long variantId);
}
