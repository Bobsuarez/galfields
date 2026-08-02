package co.com.galfields.pos_transactions.api.inventory;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentItemRequest(
        @NotNull Long variantId,
        @NotNull Integer quantityDelta) {
}
