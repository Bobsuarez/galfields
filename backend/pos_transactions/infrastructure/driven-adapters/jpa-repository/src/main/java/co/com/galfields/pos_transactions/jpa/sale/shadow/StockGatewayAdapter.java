package co.com.galfields.pos_transactions.jpa.sale.shadow;

import co.com.galfields.pos_transactions.model.sale.StockAdjustmentOutcome;
import co.com.galfields.pos_transactions.model.sale.StockDelta;
import co.com.galfields.pos_transactions.model.sale.gateways.StockGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Mirrors backend/pos's InventoryService#applyOne: idempotent per
 * (clientEventId, variantId), negative resulting stock allowed (the
 * physical sale already happened, recording an oversell truthfully is more
 * useful than rejecting it). */
@Repository
@RequiredArgsConstructor
public class StockGatewayAdapter implements StockGateway {

    private final InventoryShadowJpaRepository inventoryRepository;
    private final StockAdjustmentShadowJpaRepository stockAdjustmentRepository;

    @Override
    public List<StockAdjustmentOutcome> applyAdjustments(String clientEventId, Long locationId, List<StockDelta> deltas) {
        return deltas.stream().map(delta -> applyOne(clientEventId, locationId, delta)).toList();
    }

    private StockAdjustmentOutcome applyOne(String clientEventId, Long locationId, StockDelta delta) {
        if (stockAdjustmentRepository.existsByClientEventIdAndVariantId(clientEventId, delta.variantId())) {
            Integer resultingQuantity = inventoryRepository.findByVariantIdAndLocationId(delta.variantId(), locationId)
                    .map(InventoryShadowEntity::getQuantityOnHand)
                    .orElse(null);
            return new StockAdjustmentOutcome(delta.variantId(), true, resultingQuantity);
        }

        InventoryShadowEntity inventory = inventoryRepository.findByVariantIdAndLocationId(delta.variantId(), locationId)
                .orElseGet(() -> {
                    InventoryShadowEntity created = new InventoryShadowEntity();
                    created.setVariantId(delta.variantId());
                    created.setLocationId(locationId);
                    created.setQuantityOnHand(0);
                    return created;
                });

        int resultingQuantity = inventory.getQuantityOnHand() + delta.quantityDelta();
        inventory.setQuantityOnHand(resultingQuantity);
        inventoryRepository.save(inventory);

        StockAdjustmentShadowEntity adjustment = new StockAdjustmentShadowEntity();
        adjustment.setClientEventId(clientEventId);
        adjustment.setVariantId(delta.variantId());
        adjustment.setLocationId(locationId);
        adjustment.setQuantityDelta(delta.quantityDelta());
        adjustment.setResultingQuantity(resultingQuantity);
        stockAdjustmentRepository.save(adjustment);

        return new StockAdjustmentOutcome(delta.variantId(), false, resultingQuantity);
    }
}
