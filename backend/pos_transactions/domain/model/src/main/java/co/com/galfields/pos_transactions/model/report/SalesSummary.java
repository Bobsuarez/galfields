package co.com.galfields.pos_transactions.model.report;

import java.math.BigDecimal;

public record SalesSummary(BigDecimal totalSales, long transactionCount, BigDecimal averageTicket) {
}
