package co.com.galfields.pos_transactions.jpa.report;

import java.math.BigDecimal;

public interface InvoicePaymentProjection {
    String getMethodName();

    BigDecimal getAmount();

    String getReferenceNumber();
}
