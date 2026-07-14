package co.com.galfields.pos.repository;

import co.com.galfields.pos.dto.InventoryRowResponse;
import co.com.galfields.pos.entity.Inventory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariant_VariantIdAndLocation_LocationId(Long variantId, Long locationId);

    @Query("""
            SELECT new co.com.galfields.pos.dto.InventoryRowResponse(
                v.variantId, v.sku, p.name, c.name, l.name, i.quantityOnHand)
            FROM Inventory i
            JOIN i.variant v
            JOIN v.product p
            LEFT JOIN p.category c
            JOIN i.location l
            ORDER BY p.name, v.sku
            """)
    Page<InventoryRowResponse> findAllRows(Pageable pageable);

    @Query("""
            SELECT new co.com.galfields.pos.dto.InventoryRowResponse(
                v.variantId, v.sku, p.name, c.name, l.name, i.quantityOnHand)
            FROM Inventory i
            JOIN i.variant v
            JOIN v.product p
            LEFT JOIN p.category c
            JOIN i.location l
            WHERE i.quantityOnHand <= :threshold
            ORDER BY i.quantityOnHand ASC
            """)
    Page<InventoryRowResponse> findLowStockRows(@Param("threshold") int threshold, Pageable pageable);
}
