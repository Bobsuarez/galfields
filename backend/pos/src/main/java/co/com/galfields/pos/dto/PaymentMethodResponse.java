package co.com.galfields.pos.dto;

import java.time.LocalDateTime;

public record PaymentMethodResponse(
        Long paymentMethodId,
        String methodName,
        boolean active,
        LocalDateTime createdAt) {
}
