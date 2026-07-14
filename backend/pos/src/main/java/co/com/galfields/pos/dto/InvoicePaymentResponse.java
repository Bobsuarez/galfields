package co.com.galfields.pos.dto;

import java.math.BigDecimal;

public record InvoicePaymentResponse(
        String methodName,
        BigDecimal amount,
        String referenceNumber) {
}
