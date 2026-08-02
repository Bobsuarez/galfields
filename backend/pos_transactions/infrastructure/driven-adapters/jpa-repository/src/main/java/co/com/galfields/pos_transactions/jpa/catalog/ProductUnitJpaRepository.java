package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductUnitJpaRepository extends JpaRepository<ProductUnitEntity, Long> {
    List<ProductUnitEntity> findByVariantId(Long variantId);

    Optional<ProductUnitEntity> findByProductUnitIdAndVariantId(Long productUnitId, Long variantId);
}
