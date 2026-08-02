package co.com.galfields.pos_transactions.usecase.employee;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.model.employee.gateways.TerminalRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Mirrors backend/pos's TerminalService. */
@RequiredArgsConstructor
public class TerminalUseCase {

    private final TerminalRepository terminalRepository;

    public Terminal create(Terminal terminal) {
        return terminalRepository.save(terminal);
    }

    public Terminal get(Long terminalId) {
        return findOrThrow(terminalId);
    }

    public List<Terminal> list() {
        return terminalRepository.findAllOrderByCode();
    }

    public Terminal update(Long terminalId, Terminal patch) {
        Terminal existing = findOrThrow(terminalId);
        existing.setTerminalCode(patch.getTerminalCode());
        existing.setName(patch.getName());
        existing.setActive(patch.isActive());
        return terminalRepository.save(existing);
    }

    public void delete(Long terminalId) {
        findOrThrow(terminalId);
        terminalRepository.deleteById(terminalId);
    }

    private Terminal findOrThrow(Long terminalId) {
        return terminalRepository.findById(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal " + terminalId + " not found"));
    }
}
