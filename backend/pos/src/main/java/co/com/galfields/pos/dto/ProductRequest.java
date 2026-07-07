package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        String description,
        Long categoryId,
        Long brandId,
        @NotBlank String sku,
        @NotBlank String barcode,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull @PositiveOrZero BigDecimal costPrice,
        @PositiveOrZero Integer initialStock) {
}
