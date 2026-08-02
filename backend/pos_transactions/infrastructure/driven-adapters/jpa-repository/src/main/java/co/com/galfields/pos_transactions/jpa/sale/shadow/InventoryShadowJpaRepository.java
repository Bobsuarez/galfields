package co.com.galfields.pos_transactions.jpa.sale.shadow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryShadowJpaRepository extends JpaRepository<InventoryShadowEntity, Long> {
    Optional<InventoryShadowEntity> findByVariantIdAndLocationId(Long variantId, Long locationId);
}
