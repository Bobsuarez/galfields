package co.com.galfields.pos_transactions.jpa.report;

import java.math.BigDecimal;

public interface SalesSummaryProjection {
    BigDecimal getTotalSales();

    Long getTransactionCount();

    BigDecimal getAverageTicket();
}
