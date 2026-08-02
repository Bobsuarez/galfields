package co.com.galfields.pos_transactions.api.report;

import java.math.BigDecimal;

public record InvoiceLineResponse(
        String productName,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String unitName,
        int conversionFactor) {
}
