package co.com.galfields.pos_transactions.api.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceSummaryResponse(
        Long transactionId,
        LocalDateTime transactionDate,
        String employeeName,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        long itemCount,
        LocalDateTime cancelledAt,
        String invoicePrefix,
        String invoiceNumber) {
}
