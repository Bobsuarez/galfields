package co.com.galfields.pos_transactions.api.catalog;

import jakarta.validation.constraints.NotBlank;

public record VariantAttributeRequest(
        @NotBlank String name,
        @NotBlank String value) {
}
