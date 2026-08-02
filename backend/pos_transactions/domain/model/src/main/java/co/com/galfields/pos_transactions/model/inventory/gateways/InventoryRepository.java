package co.com.galfields.pos_transactions.model.inventory.gateways;

import co.com.galfields.pos_transactions.model.inventory.Inventory;

import java.util.Optional;

public interface InventoryRepository {
    Optional<Inventory> findByVariantAndLocation(Long variantId, Long locationId);

    Inventory save(Inventory inventory);
}
