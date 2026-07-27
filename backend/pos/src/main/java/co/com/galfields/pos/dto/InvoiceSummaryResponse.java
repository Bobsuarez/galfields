package co.com.galfields.pos.dto;

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
        // Null for transactions reported before V8__sales_invoice_number.sql
        // existed - callers fall back to transactionId for those.
        String invoicePrefix,
        String invoiceNumber) {
}
