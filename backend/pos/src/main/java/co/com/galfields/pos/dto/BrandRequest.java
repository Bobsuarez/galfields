package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;

public record BrandRequest(
        @NotBlank String name) {
}
