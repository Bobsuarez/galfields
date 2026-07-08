package co.com.galfields.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record ProductVariantRequest(
        @NotBlank String sku,
        @NotBlank String barcode,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull @PositiveOrZero BigDecimal costPrice,
        @PositiveOrZero Integer initialStock,
        @Valid List<VariantAttributeRequest> attributes) {
}
