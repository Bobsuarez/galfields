package co.com.galfields.pos_transactions.api.report;

import java.math.BigDecimal;

public record InvoicePaymentResponse(String methodName, BigDecimal amount, String referenceNumber) {
}
