package co.com.galfields.pos_transactions.usecase.invoicing;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.model.employee.gateways.TerminalRepository;
import co.com.galfields.pos_transactions.model.invoicing.InvoiceNumberingRange;
import co.com.galfields.pos_transactions.model.invoicing.gateways.InvoiceNumberingRangeRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Mirrors backend/pos's InvoiceNumberingRangeService. One row per terminal
 * (DB-enforced unique constraint on terminal_id — a duplicate falls through
 * to the generic DataIntegrityViolationException -> 409 handling, no
 * bespoke check needed). Reuses Fase 5's real {@link TerminalRepository}
 * (Empleados/Terminales already exists by the time this fase runs — no
 * need for a private shadow copy).
 */
@RequiredArgsConstructor
public class InvoiceNumberingRangeUseCase {

    private final InvoiceNumberingRangeRepository rangeRepository;
    private final TerminalRepository terminalRepository;

    public InvoiceNumberingRange create(InvoiceNumberingRange range) {
        resolveTerminal(range);
        return rangeRepository.save(range);
    }

    public InvoiceNumberingRange get(Long rangeId) {
        return findOrThrow(rangeId);
    }

    public List<InvoiceNumberingRange> list() {
        return rangeRepository.findAllOrderByTerminalCode();
    }

    /** What the desktop POS calls to pull its own assigned range — it only
     * knows its own terminal_code, never a numeric terminalId/rangeId. 404
     * either way, whether terminalCode itself is unknown or it's a real
     * terminal with no range assigned yet. */
    public InvoiceNumberingRange getByTerminalCode(String terminalCode) {
        Terminal terminal = terminalRepository.findByCode(terminalCode)
                .orElseThrow(() -> noRangeForTerminal(terminalCode));
        return rangeRepository.findByTerminalId(terminal.getTerminalId())
                .orElseThrow(() -> noRangeForTerminal(terminalCode));
    }

    public InvoiceNumberingRange update(Long rangeId, InvoiceNumberingRange patch) {
        InvoiceNumberingRange existing = findOrThrow(rangeId);
        existing.setTerminalId(patch.getTerminalId());
        existing.setPrefix(patch.getPrefix());
        existing.setRangeStart(patch.getRangeStart());
        existing.setRangeEnd(patch.getRangeEnd());
        resolveTerminal(existing);
        return rangeRepository.save(existing);
    }

    public void delete(Long rangeId) {
        findOrThrow(rangeId);
        rangeRepository.deleteById(rangeId);
    }

    private void resolveTerminal(InvoiceNumberingRange range) {
        Terminal terminal = terminalRepository.findById(range.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal " + range.getTerminalId() + " not found"));
        range.setTerminalCode(terminal.getTerminalCode());
    }

    private InvoiceNumberingRange findOrThrow(Long rangeId) {
        return rangeRepository.findById(rangeId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice numbering range " + rangeId + " not found"));
    }

    private ResourceNotFoundException noRangeForTerminal(String terminalCode) {
        return new ResourceNotFoundException(
                "No hay rango de facturación asignado para la terminal '" + terminalCode + "'");
    }
}
