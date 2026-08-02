package co.com.galfields.pos_transactions.jpa.inventory;

import co.com.galfields.pos_transactions.model.inventory.StockAdjustment;
import co.com.galfields.pos_transactions.model.inventory.gateways.StockAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StockAdjustmentRepositoryAdapter implements StockAdjustmentRepository {

    private final StockAdjustmentJpaRepository repository;

    @Override
    public boolean existsByClientEventIdAndVariant(String clientEventId, Long variantId) {
        return repository.existsByClientEventIdAndVariantId(clientEventId, variantId);
    }

    @Override
    public StockAdjustment save(StockAdjustment adjustment) {
        StockAdjustmentEntity entity = new StockAdjustmentEntity();
        entity.setAdjustmentId(adjustment.getAdjustmentId());
        entity.setClientEventId(adjustment.getClientEventId());
        entity.setVariantId(adjustment.getVariantId());
        entity.setLocationId(adjustment.getLocationId());
        entity.setQuantityDelta(adjustment.getQuantityDelta());
        entity.setResultingQuantity(adjustment.getResultingQuantity());

        StockAdjustmentEntity saved = repository.save(entity);

        return StockAdjustment.builder()
                .adjustmentId(saved.getAdjustmentId())
                .clientEventId(saved.getClientEventId())
                .variantId(saved.getVariantId())
                .locationId(saved.getLocationId())
                .quantityDelta(saved.getQuantityDelta())
                .resultingQuantity(saved.getResultingQuantity())
                .build();
    }
}
