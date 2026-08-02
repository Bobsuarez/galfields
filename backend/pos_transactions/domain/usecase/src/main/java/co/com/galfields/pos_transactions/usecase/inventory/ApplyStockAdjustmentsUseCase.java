package co.com.galfields.pos_transactions.usecase.inventory;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.inventory.Inventory;
import co.com.galfields.pos_transactions.model.inventory.StockAdjustment;
import co.com.galfields.pos_transactions.model.inventory.StockAdjustmentItem;
import co.com.galfields.pos_transactions.model.inventory.StockAdjustmentResult;
import co.com.galfields.pos_transactions.model.inventory.gateways.InventoryRepository;
import co.com.galfields.pos_transactions.model.inventory.gateways.LocationReferenceGateway;
import co.com.galfields.pos_transactions.model.inventory.gateways.ProductVariantReferenceGateway;
import co.com.galfields.pos_transactions.model.inventory.gateways.StockAdjustmentRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Mirrors backend/pos's InventoryService#applyAdjustments/applyOne:
 * idempotent per (clientEventId, variantId) — a retried batch replays the
 * same result instead of double-applying. Negative resulting stock is
 * allowed, not rejected: the physical event already happened by the time
 * this runs (e.g. two terminals both sold the last unit before either
 * synced), so recording an oversell truthfully is more useful than
 * rejecting a call that can't undo something already real. Scoped to the
 * single default location every inventory write in this app uses today —
 * same placeholder as RecordSaleUseCase (usecase.sale), no per-request
 * location yet.
 */
@RequiredArgsConstructor
public class ApplyStockAdjustmentsUseCase {

    private static final String DEFAULT_LOCATION_NAME = "Bogotá - Chapinero";

    private final InventoryRepository inventoryRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final LocationReferenceGateway locationReferenceGateway;
    private final ProductVariantReferenceGateway productVariantReferenceGateway;

    public List<StockAdjustmentResult> execute(String clientEventId, List<StockAdjustmentItem> items) {
        Long locationId = locationReferenceGateway.findIdByName(DEFAULT_LOCATION_NAME)
                .orElseThrow(() -> new ResourceNotFoundException("Default location '" + DEFAULT_LOCATION_NAME + "' not found"));

        return items.stream().map(item -> applyOne(clientEventId, locationId, item)).toList();
    }

    private StockAdjustmentResult applyOne(String clientEventId, Long locationId, StockAdjustmentItem item) {
        if (stockAdjustmentRepository.existsByClientEventIdAndVariant(clientEventId, item.variantId())) {
            Integer resultingQuantity = inventoryRepository.findByVariantAndLocation(item.variantId(), locationId)
                    .map(Inventory::getQuantityOnHand)
                    .orElse(null);
            return new StockAdjustmentResult(item.variantId(), true, resultingQuantity);
        }

        if (!productVariantReferenceGateway.existsById(item.variantId())) {
            throw new ResourceNotFoundException("Variant " + item.variantId() + " not found");
        }

        Inventory inventory = inventoryRepository.findByVariantAndLocation(item.variantId(), locationId)
                .orElseGet(() -> Inventory.builder()
                        .variantId(item.variantId())
                        .locationId(locationId)
                        .quantityOnHand(0)
                        .build());

        int resultingQuantity = inventory.getQuantityOnHand() + item.quantityDelta();
        inventory.setQuantityOnHand(resultingQuantity);
        inventoryRepository.save(inventory);

        stockAdjustmentRepository.save(StockAdjustment.builder()
                .clientEventId(clientEventId)
                .variantId(item.variantId())
                .locationId(locationId)
                .quantityDelta(item.quantityDelta())
                .resultingQuantity(resultingQuantity)
                .build());

        return new StockAdjustmentResult(item.variantId(), false, resultingQuantity);
    }
}
