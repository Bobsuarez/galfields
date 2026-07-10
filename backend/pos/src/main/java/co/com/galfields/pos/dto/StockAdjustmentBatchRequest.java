package co.com.galfields.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * One call per sale: {@code clientEventId} is a client-generated id for the
 * whole sale (the POS's local sale UUID), shared by every item in it -
 * paired with each item's {@code variantId} it's what makes a retried batch
 * idempotent (see InventoryService).
 */
public record StockAdjustmentBatchRequest(
        @NotBlank String clientEventId,
        @NotEmpty @Valid List<StockAdjustmentItemRequest> items) {
}
