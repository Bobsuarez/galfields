package co.com.galfields.pos_transactions.jpa.report;

public interface InventoryRowProjection {
    Long getVariantId();

    String getSku();

    String getProductName();

    String getCategoryName();

    String getLocationName();

    Integer getQuantityOnHand();
}
