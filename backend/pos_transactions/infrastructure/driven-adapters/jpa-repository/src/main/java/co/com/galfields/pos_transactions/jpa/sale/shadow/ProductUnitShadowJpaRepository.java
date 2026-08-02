package co.com.galfields.pos_transactions.jpa.sale.shadow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductUnitShadowJpaRepository extends JpaRepository<ProductUnitShadowEntity, Long> {
    Optional<ProductUnitShadowEntity> findByProductUnitIdAndVariantId(Long productUnitId, Long variantId);
}
