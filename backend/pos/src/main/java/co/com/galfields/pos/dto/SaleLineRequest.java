package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SaleLineRequest(
        @NotNull Long variantId,
        Long productUnitId,
        @NotNull Integer quantity,
        @NotNull BigDecimal unitPrice,
        @NotNull BigDecimal subtotal) {
}
