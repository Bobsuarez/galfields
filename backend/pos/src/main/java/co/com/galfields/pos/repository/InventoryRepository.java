package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.Inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariant_VariantIdAndLocation_LocationId(Long variantId, Long locationId);
}
