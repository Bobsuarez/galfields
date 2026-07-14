package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.SaleItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findByTransaction_TransactionId(Long transactionId);
}
