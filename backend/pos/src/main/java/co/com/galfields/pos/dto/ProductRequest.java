package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String name,
        String description,
        // products.category_id / brand_id are declared BIGSERIAL in the deployed
        // schema, which Postgres always makes NOT NULL regardless of intent -
        // both are mandatory even though this looks like it should be optional.
        @NotNull Long categoryId,
        @NotNull Long brandId) {
}
