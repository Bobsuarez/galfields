package co.com.galfields.pos_transactions.model.report;

public record InventoryRow(
        Long variantId,
        String sku,
        String productName,
        String categoryName,
        String locationName,
        int quantityOnHand) {
}
