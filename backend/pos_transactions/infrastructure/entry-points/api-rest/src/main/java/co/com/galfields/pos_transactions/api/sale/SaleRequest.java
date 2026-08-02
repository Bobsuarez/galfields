package co.com.galfields.pos_transactions.api.sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * One call per completed sale, reported by a POS terminal. Idempotent by
 * clientEventId — see RecordSaleUseCase. Mirrors backend/pos's SaleRequest.
 */
public record SaleRequest(
        @NotBlank String clientEventId,
        @NotEmpty @Valid List<SaleLineRequest> items,
        @NotEmpty @Valid List<SalePaymentRequest> payments,
        @NotNull BigDecimal discountAmount,
        @NotNull BigDecimal totalAmount,
        String invoicePrefix,
        String invoiceNumber) {
}
