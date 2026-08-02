package co.com.galfields.pos_transactions.api.catalog;

import jakarta.validation.constraints.NotBlank;

public record LocationRequest(
        @NotBlank String name,
        String address,
        String phone) {
}
