package co.com.galfields.pos_transactions.jpa.sale.shadow;

import co.com.galfields.pos_transactions.model.sale.ProductUnitReference;
import co.com.galfields.pos_transactions.model.sale.gateways.ProductUnitReferenceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SaleProductUnitReferenceGatewayAdapter implements ProductUnitReferenceGateway {

    private final ProductUnitShadowJpaRepository repository;

    @Override
    public Optional<ProductUnitReference> find(Long productUnitId, Long variantId) {
        return repository.findByProductUnitIdAndVariantId(productUnitId, variantId)
                .map(entity -> new ProductUnitReference(entity.getUnitName(), entity.getConversionFactor()));
    }
}
