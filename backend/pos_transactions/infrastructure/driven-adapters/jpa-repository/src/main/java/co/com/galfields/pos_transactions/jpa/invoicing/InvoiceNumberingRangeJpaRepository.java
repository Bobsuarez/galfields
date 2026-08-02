package co.com.galfields.pos_transactions.jpa.invoicing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceNumberingRangeJpaRepository extends JpaRepository<InvoiceNumberingRangeEntity, Long> {
    Optional<InvoiceNumberingRangeEntity> findByTerminalId(Long terminalId);
}
