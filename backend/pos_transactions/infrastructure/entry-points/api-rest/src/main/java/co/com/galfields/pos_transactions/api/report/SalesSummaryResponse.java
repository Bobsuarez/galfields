package co.com.galfields.pos_transactions.api.report;

import java.math.BigDecimal;

public record SalesSummaryResponse(BigDecimal totalSales, long transactionCount, BigDecimal averageTicket) {
}
