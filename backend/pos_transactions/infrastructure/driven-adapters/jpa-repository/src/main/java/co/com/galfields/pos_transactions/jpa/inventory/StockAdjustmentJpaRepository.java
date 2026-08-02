package co.com.galfields.pos_transactions.jpa.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentJpaRepository extends JpaRepository<StockAdjustmentEntity, Long> {
    boolean existsByClientEventIdAndVariantId(String clientEventId, Long variantId);
}
