package co.com.galfields.pos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductUnitRequest(
        @NotBlank String unitName,
        @NotNull @Min(1) Integer conversionFactor,
        @NotNull @PositiveOrZero BigDecimal unitPrice,
        String barcode) {
}
