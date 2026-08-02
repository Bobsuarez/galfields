package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodImageJpaRepository extends JpaRepository<PaymentMethodImageEntity, Long> {
    Optional<PaymentMethodImageEntity> findByPaymentMethodId(Long paymentMethodId);
}
