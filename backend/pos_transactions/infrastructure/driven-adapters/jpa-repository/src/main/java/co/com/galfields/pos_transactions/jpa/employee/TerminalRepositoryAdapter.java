package co.com.galfields.pos_transactions.jpa.employee;

import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.model.employee.gateways.TerminalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TerminalRepositoryAdapter implements TerminalRepository {

    private final TerminalJpaRepository repository;

    @Override
    public Optional<Terminal> findById(Long terminalId) {
        return repository.findById(terminalId).map(this::toDomain);
    }

    @Override
    public Optional<Terminal> findByCode(String terminalCode) {
        return repository.findByTerminalCode(terminalCode).map(this::toDomain);
    }

    @Override
    public List<Terminal> findAllExisting(List<Long> terminalIds) {
        return repository.findAllById(terminalIds).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Terminal> findAllOrderByCode() {
        return repository.findAllByOrderByTerminalCodeAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public Terminal save(Terminal terminal) {
        TerminalEntity entity = new TerminalEntity();
        entity.setTerminalId(terminal.getTerminalId());
        entity.setTerminalCode(terminal.getTerminalCode());
        entity.setName(terminal.getName());
        entity.setActive(terminal.isActive());
        entity.setCreatedAt(terminal.getCreatedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long terminalId) {
        repository.deleteById(terminalId);
    }

    private Terminal toDomain(TerminalEntity entity) {
        return Terminal.builder()
                .terminalId(entity.getTerminalId())
                .terminalCode(entity.getTerminalCode())
                .name(entity.getName())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
