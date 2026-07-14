package co.com.galfields.pos.dto;

import java.math.BigDecimal;

public record PaymentMethodSalesResponse(
        Long paymentMethodId,
        String methodName,
        BigDecimal totalAmount,
        long transactionCount) {
}
