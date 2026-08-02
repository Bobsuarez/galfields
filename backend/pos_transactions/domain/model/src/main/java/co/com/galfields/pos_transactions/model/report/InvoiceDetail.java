package co.com.galfields.pos_transactions.model.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDetail(
        Long transactionId,
        LocalDateTime transactionDate,
        String employeeName,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        List<InvoiceLine> items,
        List<InvoicePayment> payments,
        LocalDateTime cancelledAt,
        String invoicePrefix,
        String invoiceNumber) {
}
