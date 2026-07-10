package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentItemRequest(
        @NotNull Long variantId,
        @NotNull Integer quantityDelta) {
}
