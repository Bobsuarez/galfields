package co.com.galfields.pos_transactions.jpa.catalog;

import co.com.galfields.pos_transactions.model.catalog.ProductUnit;
import co.com.galfields.pos_transactions.model.catalog.gateways.ProductUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductUnitRepositoryAdapter implements ProductUnitRepository {

    private final ProductUnitJpaRepository repository;

    @Override
    public Optional<ProductUnit> findByIdAndVariantId(Long productUnitId, Long variantId) {
        return repository.findByProductUnitIdAndVariantId(productUnitId, variantId).map(this::toDomain);
    }

    @Override
    public ProductUnit save(ProductUnit unit) {
        ProductUnitEntity entity = new ProductUnitEntity();
        entity.setProductUnitId(unit.getProductUnitId());
        entity.setVariantId(unit.getVariantId());
        entity.setUnitName(unit.getUnitName());
        entity.setConversionFactor(unit.getConversionFactor());
        entity.setUnitPrice(unit.getUnitPrice());
        entity.setBarcode(unit.getBarcode());
        entity.setBase(unit.isBase());
        entity.setActive(unit.isActive());
        return toDomain(repository.save(entity));
    }

    private ProductUnit toDomain(ProductUnitEntity entity) {
        return ProductUnit.builder()
                .productUnitId(entity.getProductUnitId())
                .variantId(entity.getVariantId())
                .unitName(entity.getUnitName())
                .conversionFactor(entity.getConversionFactor())
                .unitPrice(entity.getUnitPrice())
                .barcode(entity.getBarcode())
                .base(entity.isBase())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
