package co.com.galfields.pos_transactions.jpa.sale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleItemJpaRepository extends JpaRepository<SaleItemEntity, Long> {
    List<SaleItemEntity> findByTransactionId(Long transactionId);
}
