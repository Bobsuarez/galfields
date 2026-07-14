package co.com.galfields.pos.dto;

import java.math.BigDecimal;

public record InvoiceLineResponse(
        String productName,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {
}
