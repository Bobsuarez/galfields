package co.com.galfields.pos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long productId,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ProductVariantResponse> variants) {
}
