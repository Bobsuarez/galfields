package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.StockAdjustmentBatchRequest;
import co.com.galfields.pos.dto.StockAdjustmentBatchResponse;
import co.com.galfields.pos.dto.StockAdjustmentItemRequest;
import co.com.galfields.pos.dto.StockAdjustmentResult;
import co.com.galfields.pos.entity.Inventory;
import co.com.galfields.pos.entity.Location;
import co.com.galfields.pos.entity.ProductVariant;
import co.com.galfields.pos.entity.StockAdjustment;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.InventoryRepository;
import co.com.galfields.pos.repository.LocationRepository;
import co.com.galfields.pos.repository.ProductVariantRepository;
import co.com.galfields.pos.repository.StockAdjustmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies stock deltas reported by POS terminals (a sale decrements; a
 * future return/manual correction could increment) against {@code
 * inventory}, scoped to the same hardcoded default location every other
 * inventory write in this app uses today (see
 * ProductService.DEFAULT_LOCATION_NAME) - multi-location POS terminals are a
 * bigger, separate change.
 *
 * The physical sale already happened by the time this is called, so a
 * resulting negative quantity is recorded as-is rather than rejected - it
 * reflects an oversell (e.g. two terminals sold the last unit before either
 * synced), not an error to undo.
 *
 * Idempotent per (clientEventId, variantId): a POS retrying a batch it's
 * already reported (e.g. it applied locally but never saw the response)
 * gets the same result back without double-applying.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String DEFAULT_LOCATION_NAME = "Bodega Principal";

    private final ProductVariantRepository productVariantRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    @Transactional
    public StockAdjustmentBatchResponse applyAdjustments(StockAdjustmentBatchRequest request) {
        Location location = defaultLocation();

        List<StockAdjustmentResult> results = request.items().stream()
                .map(item -> applyOne(request.clientEventId(), location, item))
                .toList();

        return new StockAdjustmentBatchResponse(request.clientEventId(), results);
    }

    private StockAdjustmentResult applyOne(String clientEventId, Location location, StockAdjustmentItemRequest item) {
        if (stockAdjustmentRepository.existsByClientEventIdAndVariant_VariantId(clientEventId, item.variantId())) {
            Integer resultingQuantity = inventoryRepository
                    .findByVariant_VariantIdAndLocation_LocationId(item.variantId(), location.getLocationId())
                    .map(Inventory::getQuantityOnHand)
                    .orElse(null);
            return new StockAdjustmentResult(item.variantId(), true, resultingQuantity);
        }

        ProductVariant variant = productVariantRepository.findById(item.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant " + item.variantId() + " not found"));

        Inventory inventory = inventoryRepository
                .findByVariant_VariantIdAndLocation_LocationId(item.variantId(), location.getLocationId())
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setVariant(variant);
                    created.setLocation(location);
                    created.setQuantityOnHand(0);
                    return created;
                });

        int resultingQuantity = inventory.getQuantityOnHand() + item.quantityDelta();
        inventory.setQuantityOnHand(resultingQuantity);
        inventoryRepository.save(inventory);

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setClientEventId(clientEventId);
        adjustment.setVariant(variant);
        adjustment.setLocation(location);
        adjustment.setQuantityDelta(item.quantityDelta());
        adjustment.setResultingQuantity(resultingQuantity);
        stockAdjustmentRepository.save(adjustment);

        return new StockAdjustmentResult(item.variantId(), false, resultingQuantity);
    }

    private Location defaultLocation() {
        return locationRepository.findByName(DEFAULT_LOCATION_NAME)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Default location '" + DEFAULT_LOCATION_NAME + "' not found"));
    }
}
