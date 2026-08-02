package co.com.galfields.pos_transactions.model.invoicing.gateways;

import co.com.galfields.pos_transactions.model.invoicing.InvoiceNumberingRange;

import java.util.List;
import java.util.Optional;

public interface InvoiceNumberingRangeRepository {
    Optional<InvoiceNumberingRange> findById(Long rangeId);

    Optional<InvoiceNumberingRange> findByTerminalId(Long terminalId);

    List<InvoiceNumberingRange> findAllOrderByTerminalCode();

    InvoiceNumberingRange save(InvoiceNumberingRange range);

    void deleteById(Long rangeId);
}
