package co.com.galfields.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        @NotNull BigDecimal totalAmount,
        // Snapshotted DIAN invoice number - see apps/galfield-pos's
        // invoices.rs::create_sale. Not @NotBlank: a terminal that hasn't
        // updated yet (or has no numbering range configured) still has to
        // be able to report a sale; those land with null invoice fields.
        String invoicePrefix,
        String invoiceNumber,
        // The actual wall-clock moment the sale happened, snapshotted by the
        // terminal (apps/galfield-pos's sales_sync.rs) from its local
        // sales.created_at, with an explicit offset so it's unambiguous
        // regardless of the terminal's own clock config. Nullable: a
        // terminal that hasn't updated yet omits it, and SalesService falls
        // back to the moment the cloud received the report (the only
        // behavior that existed before this field) - see
        // specs/01-reportes-mobile-pos-zona-horaria.md.
        OffsetDateTime transactionDate) {
}
