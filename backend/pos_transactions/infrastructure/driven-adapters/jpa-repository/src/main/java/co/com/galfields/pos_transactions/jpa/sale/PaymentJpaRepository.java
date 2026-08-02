package co.com.galfields.pos_transactions.jpa.sale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByTransactionId(Long transactionId);
}
