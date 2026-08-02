package co.com.galfields.pos_transactions.model.inventory.gateways;

import co.com.galfields.pos_transactions.model.inventory.StockAdjustment;

public interface StockAdjustmentRepository {
    boolean existsByClientEventIdAndVariant(String clientEventId, Long variantId);

    StockAdjustment save(StockAdjustment adjustment);
}
