package co.com.galfields.pos_transactions.api.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * One call per sale (or any batch of stock deltas): clientEventId is the
 * client-generated id shared by every item in it — paired with each item's
 * variantId, it's what makes a retried batch idempotent. Mirrors
 * backend/pos's StockAdjustmentBatchRequest.
 */
public record StockAdjustmentBatchRequest(
        @NotBlank String clientEventId,
        @NotEmpty @Valid List<StockAdjustmentItemRequest> items) {
}
