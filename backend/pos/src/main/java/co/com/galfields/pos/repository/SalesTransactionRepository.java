package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.SalesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {
}
