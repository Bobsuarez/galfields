package co.com.galfields.pos.dto;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        BigDecimal totalSales,
        long transactionCount,
        BigDecimal averageTicket) {
}
