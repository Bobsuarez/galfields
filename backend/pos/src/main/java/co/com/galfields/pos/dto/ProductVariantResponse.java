package co.com.galfields.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductVariantResponse(
        Long variantId,
        String sku,
        String barcode,
        BigDecimal price,
        BigDecimal costPrice,
        Integer stock,
        String imageUrl,
        boolean active,
        List<VariantAttributeResponse> attributes) {
}
