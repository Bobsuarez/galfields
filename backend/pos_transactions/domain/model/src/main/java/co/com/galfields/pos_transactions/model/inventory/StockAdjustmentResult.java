package co.com.galfields.pos_transactions.model.inventory;

public record StockAdjustmentResult(Long variantId, boolean alreadyProcessed, Integer resultingQuantity) {
}
