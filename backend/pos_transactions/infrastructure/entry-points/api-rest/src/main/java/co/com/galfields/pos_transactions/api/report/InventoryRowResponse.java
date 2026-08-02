package co.com.galfields.pos_transactions.api.report;

public record InventoryRowResponse(
        Long variantId,
        String sku,
        String productName,
        String categoryName,
        String locationName,
        int quantityOnHand) {
}
