package co.com.galfields.pos_transactions.jpa.sale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesTransactionJpaRepository extends JpaRepository<SalesTransactionEntity, Long> {
    Optional<SalesTransactionEntity> findByClientEventId(String clientEventId);
}
