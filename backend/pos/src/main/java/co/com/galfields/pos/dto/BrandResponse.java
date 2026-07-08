package co.com.galfields.pos.dto;

import java.time.LocalDateTime;

public record BrandResponse(
        Long brandId,
        String name,
        LocalDateTime createdAt) {
}
