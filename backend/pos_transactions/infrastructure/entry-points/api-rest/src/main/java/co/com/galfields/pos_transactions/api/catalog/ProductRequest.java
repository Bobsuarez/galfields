package co.com.galfields.pos_transactions.api.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull Long categoryId,
        @NotNull Long brandId) {
}
