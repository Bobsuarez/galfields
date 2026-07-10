package co.com.galfields.pos.dto;

public record StockAdjustmentResult(
        Long variantId,
        boolean alreadyProcessed,
        Integer resultingQuantity) {
}
