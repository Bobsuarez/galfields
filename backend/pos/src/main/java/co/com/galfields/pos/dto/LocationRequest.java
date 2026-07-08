package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;

public record LocationRequest(
        @NotBlank String name,
        String address,
        String phone) {
}
