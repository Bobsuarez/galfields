package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantAttributeJpaRepository extends JpaRepository<VariantAttributeEntity, Long> {
    List<VariantAttributeEntity> findByVariantId(Long variantId);
}
