package co.com.galfields.pos_transactions.api.inventory;

public record StockAdjustmentResultResponse(
        Long variantId,
        boolean alreadyProcessed,
        Integer resultingQuantity) {
}
