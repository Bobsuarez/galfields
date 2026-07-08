package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;

public record VariantAttributeRequest(
        @NotBlank String name,
        @NotBlank String value) {
}
