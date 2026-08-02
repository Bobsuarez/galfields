package co.com.galfields.pos_transactions.model.report;

import java.math.BigDecimal;

public record InvoicePayment(String methodName, BigDecimal amount, String referenceNumber) {
}
