package co.com.galfields.pos_transactions.api.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentMethodRequest(
        @NotBlank String methodName,
        @NotNull Boolean active) {
}
