package co.com.galfields.pos_transactions.model.employee.gateways;

import co.com.galfields.pos_transactions.model.employee.Terminal;

import java.util.List;
import java.util.Optional;

public interface TerminalRepository {
    Optional<Terminal> findById(Long terminalId);

    Optional<Terminal> findByCode(String terminalCode);

    List<Terminal> findAllExisting(List<Long> terminalIds);

    List<Terminal> findAllOrderByCode();

    Terminal save(Terminal terminal);

    void deleteById(Long terminalId);
}
