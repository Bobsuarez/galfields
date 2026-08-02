package co.com.galfields.pos_transactions.jpa.report;

import co.com.galfields.pos_transactions.jpa.inventory.InventoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportInventoryJpaRepository extends JpaRepository<InventoryEntity, Long> {

    @Query(nativeQuery = true, value = """
            SELECT
                v.variant_id AS variant_id,
                v.sku AS sku,
                p.name AS product_name,
                c.name AS category_name,
                l.name AS location_name,
                i.quantity_on_hand AS quantity_on_hand
            FROM inventory i
            JOIN product_variants v ON v.variant_id = i.variant_id
            JOIN products p ON p.product_id = v.product_id
            LEFT JOIN categories c ON c.category_id = p.category_id
            JOIN locations l ON l.location_id = i.location_id
            ORDER BY p.name, v.sku
            """,
            countQuery = "SELECT COUNT(*) FROM inventory")
    Page<InventoryRowProjection> findAllRows(Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT
                v.variant_id AS variant_id,
                v.sku AS sku,
                p.name AS product_name,
                c.name AS category_name,
                l.name AS location_name,
                i.quantity_on_hand AS quantity_on_hand
            FROM inventory i
            JOIN product_variants v ON v.variant_id = i.variant_id
            JOIN products p ON p.product_id = v.product_id
            LEFT JOIN categories c ON c.category_id = p.category_id
            JOIN locations l ON l.location_id = i.location_id
            WHERE i.quantity_on_hand <= :threshold
            ORDER BY i.quantity_on_hand ASC
            """,
            countQuery = "SELECT COUNT(*) FROM inventory i WHERE i.quantity_on_hand <= :threshold")
    Page<InventoryRowProjection> findLowStockRows(@Param("threshold") int threshold, Pageable pageable);
}
