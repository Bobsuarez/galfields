package co.com.galfields.pos_transactions.jpa.sale.shadow;

import co.com.galfields.pos_transactions.model.sale.gateways.ProductVariantReferenceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SaleProductVariantReferenceGatewayAdapter implements ProductVariantReferenceGateway {

    private final ProductVariantShadowJpaRepository repository;

    @Override
    public boolean existsById(Long variantId) {
        return repository.existsById(variantId);
    }
}
