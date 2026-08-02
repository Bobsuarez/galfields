package co.com.galfields.pos_transactions.jpa.sale.shadow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantShadowJpaRepository extends JpaRepository<ProductVariantShadowEntity, Long> {
}
