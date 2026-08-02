package co.com.galfields.pos_transactions.api.catalog;

import java.time.LocalDateTime;

public record LocationResponse(
        Long locationId,
        String name,
        String address,
        String phone,
        LocalDateTime createdAt) {
}
