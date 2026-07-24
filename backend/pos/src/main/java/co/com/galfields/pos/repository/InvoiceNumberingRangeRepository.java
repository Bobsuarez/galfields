package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.InvoiceNumberingRange;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceNumberingRangeRepository extends JpaRepository<InvoiceNumberingRange, Long> {

    List<InvoiceNumberingRange> findAllByOrderByTerminalCodeAsc();

    Optional<InvoiceNumberingRange> findByTerminalCode(String terminalCode);
}
