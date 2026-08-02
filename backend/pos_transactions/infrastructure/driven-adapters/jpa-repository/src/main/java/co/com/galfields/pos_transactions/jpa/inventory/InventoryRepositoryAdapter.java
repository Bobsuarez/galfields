package co.com.galfields.pos_transactions.jpa.inventory;

import co.com.galfields.pos_transactions.model.inventory.Inventory;
import co.com.galfields.pos_transactions.model.inventory.gateways.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final InventoryJpaRepository repository;

    @Override
    public Optional<Inventory> findByVariantAndLocation(Long variantId, Long locationId) {
        return repository.findByVariantIdAndLocationId(variantId, locationId).map(this::toDomain);
    }

    @Override
    public Inventory save(Inventory inventory) {
        return toDomain(repository.save(toEntity(inventory)));
    }

    private Inventory toDomain(InventoryEntity entity) {
        return Inventory.builder()
                .inventoryId(entity.getInventoryId())
                .variantId(entity.getVariantId())
                .locationId(entity.getLocationId())
                .quantityOnHand(entity.getQuantityOnHand())
                .build();
    }

    private InventoryEntity toEntity(Inventory inventory) {
        InventoryEntity entity = new InventoryEntity();
        entity.setInventoryId(inventory.getInventoryId());
        entity.setVariantId(inventory.getVariantId());
        entity.setLocationId(inventory.getLocationId());
        entity.setQuantityOnHand(inventory.getQuantityOnHand());
        return entity;
    }
}
