package co.com.galfields.pos_transactions.jpa.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface InvoiceSummaryProjection {
    Long getTransactionId();

    LocalDateTime getTransactionDate();

    String getEmployeeName();

    BigDecimal getTotalAmount();

    BigDecimal getDiscountAmount();

    Long getItemCount();

    LocalDateTime getCancelledAt();

    String getInvoicePrefix();

    String getInvoiceNumber();
}
