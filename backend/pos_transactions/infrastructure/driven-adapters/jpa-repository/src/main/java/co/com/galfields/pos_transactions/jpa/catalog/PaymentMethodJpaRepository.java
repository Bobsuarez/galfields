package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentMethodJpaRepository extends JpaRepository<PaymentMethodEntity, Long> {
    List<PaymentMethodEntity> findAllByOrderByMethodNameAsc();
}
