package co.com.galfields.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDetailResponse(
        Long transactionId,
        LocalDateTime transactionDate,
        String employeeName,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        List<InvoiceLineResponse> items,
        List<InvoicePaymentResponse> payments,
        LocalDateTime cancelledAt,
        // Null for transactions reported before V8__sales_invoice_number.sql
        // existed - callers fall back to transactionId for those.
        String invoicePrefix,
        String invoiceNumber) {
}
