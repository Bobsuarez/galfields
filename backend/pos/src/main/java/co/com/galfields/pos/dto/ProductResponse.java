package co.com.galfields.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        Long variantId,
        String sku,
        String barcode,
        BigDecimal price,
        BigDecimal costPrice,
        Integer stock,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
