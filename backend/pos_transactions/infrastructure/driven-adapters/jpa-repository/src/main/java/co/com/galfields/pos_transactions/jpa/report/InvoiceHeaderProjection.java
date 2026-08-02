package co.com.galfields.pos_transactions.jpa.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface InvoiceHeaderProjection {
    Long getTransactionId();

    LocalDateTime getTransactionDate();

    String getEmployeeName();

    BigDecimal getTotalAmount();

    BigDecimal getDiscountAmount();

    BigDecimal getTaxAmount();

    LocalDateTime getCancelledAt();

    String getInvoicePrefix();

    String getInvoiceNumber();
}
