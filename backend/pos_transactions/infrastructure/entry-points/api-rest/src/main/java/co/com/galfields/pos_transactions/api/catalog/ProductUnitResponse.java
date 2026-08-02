package co.com.galfields.pos_transactions.api.catalog;

import java.math.BigDecimal;

public record ProductUnitResponse(
        Long productUnitId,
        String unitName,
        Integer conversionFactor,
        BigDecimal unitPrice,
        Integer stock,
        String barcode,
        boolean isBase,
        boolean active) {
}
