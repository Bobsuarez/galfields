package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SalePaymentRequest(
        @NotNull Long paymentMethodId,
        @NotNull BigDecimal amount,
        String referenceNumber) {
}
