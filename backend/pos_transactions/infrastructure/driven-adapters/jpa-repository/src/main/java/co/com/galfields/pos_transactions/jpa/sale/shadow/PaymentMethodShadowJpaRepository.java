package co.com.galfields.pos_transactions.jpa.sale.shadow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodShadowJpaRepository extends JpaRepository<PaymentMethodShadowEntity, Long> {
}
