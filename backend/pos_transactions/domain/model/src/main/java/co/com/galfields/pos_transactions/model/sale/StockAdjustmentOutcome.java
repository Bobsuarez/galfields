package co.com.galfields.pos_transactions.model.sale;

public record StockAdjustmentOutcome(Long variantId, boolean alreadyProcessed, Integer resultingQuantity) {
}
