package co.com.galfields.pos_transactions.model.report;

import java.math.BigDecimal;

public record PaymentMethodSales(Long paymentMethodId, String methodName, BigDecimal totalAmount, long transactionCount) {
}
