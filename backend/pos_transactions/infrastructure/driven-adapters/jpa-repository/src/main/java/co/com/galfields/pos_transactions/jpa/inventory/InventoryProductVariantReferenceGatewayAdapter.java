package co.com.galfields.pos_transactions.jpa.inventory;

import co.com.galfields.pos_transactions.model.inventory.gateways.ProductVariantReferenceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryProductVariantReferenceGatewayAdapter implements ProductVariantReferenceGateway {

    private final ProductVariantRefJpaRepository repository;

    @Override
    public boolean existsById(Long variantId) {
        return repository.existsById(variantId);
    }
}
