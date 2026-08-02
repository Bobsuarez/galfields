package co.com.galfields.pos_transactions.model.report;

import java.math.BigDecimal;

public record InvoiceLine(
        String productName,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String unitName,
        int conversionFactor) {
}
