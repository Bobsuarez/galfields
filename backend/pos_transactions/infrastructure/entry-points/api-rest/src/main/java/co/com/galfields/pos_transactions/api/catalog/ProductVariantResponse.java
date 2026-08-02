package co.com.galfields.pos_transactions.api.catalog;

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
        List<VariantAttributeResponse> attributes,
        List<ProductUnitResponse> units) {
}
