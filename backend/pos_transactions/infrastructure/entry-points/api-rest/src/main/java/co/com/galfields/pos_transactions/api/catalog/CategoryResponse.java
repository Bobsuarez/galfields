package co.com.galfields.pos_transactions.api.catalog;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long categoryId,
        String name,
        String description,
        LocalDateTime createdAt) {
}
