package co.com.galfields.pos_transactions.api.catalog;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        String description) {
}
