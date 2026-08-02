package co.com.galfields.pos_transactions.jpa.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, Long> {
    Optional<InventoryEntity> findByVariantIdAndLocationId(Long variantId, Long locationId);
}
