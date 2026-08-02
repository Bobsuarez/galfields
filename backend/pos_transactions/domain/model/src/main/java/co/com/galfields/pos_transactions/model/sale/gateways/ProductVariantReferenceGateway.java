package co.com.galfields.pos_transactions.model.sale.gateways;

public interface ProductVariantReferenceGateway {
    boolean existsById(Long variantId);
}
