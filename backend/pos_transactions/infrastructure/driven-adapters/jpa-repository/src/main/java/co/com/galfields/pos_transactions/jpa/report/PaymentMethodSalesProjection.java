package co.com.galfields.pos_transactions.jpa.report;

import java.math.BigDecimal;

public interface PaymentMethodSalesProjection {
    Long getPaymentMethodId();

    String getMethodName();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
