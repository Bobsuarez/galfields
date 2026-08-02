package co.com.galfields.pos_transactions.api.report;

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
        String invoicePrefix,
        String invoiceNumber) {
}
