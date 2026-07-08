package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.ProductVariantImage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, Long> {

    Optional<ProductVariantImage> findByVariant_VariantId(Long variantId);
}
