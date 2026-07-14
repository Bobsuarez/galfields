package co.com.galfields.pos.dto;

public record InventoryRowResponse(
        Long variantId,
        String sku,
        String productName,
        String categoryName,
        String locationName,
        int quantityOnHand) {
}
