package co.com.galfields.pos_transactions.model.inventory.gateways;

public interface ProductVariantReferenceGateway {
    boolean existsById(Long variantId);
}
