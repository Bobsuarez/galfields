package co.com.galfields.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * One call per completed sale, reported by a POS terminal (currently only
 * {@code apps/galfield-pos} - see that repo's CLAUDE.md). Idempotent by
 * {@code clientEventId} (the terminal's local sale id): a retried report
 * (e.g. it applied locally but never saw the response) returns the
 * already-created transaction instead of duplicating it - see SalesService.
 * Applies the matching stock adjustment atomically with the sale record,
 * reusing InventoryService.applyAdjustments under the same clientEventId.
 */
public record SaleRequest(
        @NotBlank String clientEventId,
        @NotEmpty @Valid List<SaleLineRequest> items,
        @NotEmpty @Valid List<SalePaymentRequest> payments,
        @NotNull BigDecimal discountAmount,
        @NotNull BigDecimal totalAmount) {
}
