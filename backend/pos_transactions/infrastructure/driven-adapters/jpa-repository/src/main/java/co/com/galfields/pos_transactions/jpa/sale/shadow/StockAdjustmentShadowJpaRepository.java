package co.com.galfields.pos_transactions.jpa.sale.shadow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentShadowJpaRepository extends JpaRepository<StockAdjustmentShadowEntity, Long> {
    boolean existsByClientEventIdAndVariantId(String clientEventId, Long variantId);
}
