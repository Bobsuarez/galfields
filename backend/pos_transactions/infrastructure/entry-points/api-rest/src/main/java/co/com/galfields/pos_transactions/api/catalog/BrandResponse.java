package co.com.galfields.pos_transactions.api.catalog;

import java.time.LocalDateTime;

public record BrandResponse(Long brandId, String name, LocalDateTime createdAt) {
}
