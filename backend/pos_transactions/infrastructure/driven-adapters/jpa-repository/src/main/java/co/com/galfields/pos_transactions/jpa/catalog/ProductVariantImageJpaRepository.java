package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductVariantImageJpaRepository extends JpaRepository<ProductVariantImageEntity, Long> {
    Optional<ProductVariantImageEntity> findByVariantId(Long variantId);
}
