package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.ProductUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, Long> {

    List<ProductUnit> findByVariant_VariantId(Long variantId);

    Optional<ProductUnit> findByProductUnitIdAndVariant_VariantId(Long productUnitId, Long variantId);
}
