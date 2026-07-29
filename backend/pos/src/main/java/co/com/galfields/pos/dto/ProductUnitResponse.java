package co.com.galfields.pos.dto;

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
