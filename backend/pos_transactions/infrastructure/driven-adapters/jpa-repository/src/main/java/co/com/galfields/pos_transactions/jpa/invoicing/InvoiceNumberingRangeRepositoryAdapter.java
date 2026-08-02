package co.com.galfields.pos_transactions.jpa.invoicing;

import co.com.galfields.pos_transactions.jpa.employee.TerminalEntity;
import co.com.galfields.pos_transactions.jpa.employee.TerminalJpaRepository;
import co.com.galfields.pos_transactions.model.invoicing.InvoiceNumberingRange;
import co.com.galfields.pos_transactions.model.invoicing.gateways.InvoiceNumberingRangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Same Gradle module as jpa.employee, so this reuses TerminalJpaRepository
 * directly (no cross-module Gradle dependency needed) to resolve the
 * denormalized terminalCode — terminalCode isn't a column on this table. */
@Repository
@RequiredArgsConstructor
public class InvoiceNumberingRangeRepositoryAdapter implements InvoiceNumberingRangeRepository {

    private final InvoiceNumberingRangeJpaRepository repository;
    private final TerminalJpaRepository terminalRepository;

    @Override
    public Optional<InvoiceNumberingRange> findById(Long rangeId) {
        return repository.findById(rangeId).map(this::toDomain);
    }

    @Override
    public Optional<InvoiceNumberingRange> findByTerminalId(Long terminalId) {
        return repository.findByTerminalId(terminalId).map(this::toDomain);
    }

    @Override
    public List<InvoiceNumberingRange> findAllOrderByTerminalCode() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(InvoiceNumberingRange::getTerminalCode,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public InvoiceNumberingRange save(InvoiceNumberingRange range) {
        InvoiceNumberingRangeEntity entity = new InvoiceNumberingRangeEntity();
        entity.setRangeId(range.getRangeId());
        entity.setTerminalId(range.getTerminalId());
        entity.setPrefix(range.getPrefix());
        entity.setRangeStart(range.getRangeStart());
        entity.setRangeEnd(range.getRangeEnd());
        entity.setCreatedAt(range.getCreatedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long rangeId) {
        repository.deleteById(rangeId);
    }

    private InvoiceNumberingRange toDomain(InvoiceNumberingRangeEntity entity) {
        String terminalCode = terminalRepository.findById(entity.getTerminalId())
                .map(TerminalEntity::getTerminalCode)
                .orElse(null);

        return InvoiceNumberingRange.builder()
                .rangeId(entity.getRangeId())
                .terminalId(entity.getTerminalId())
                .terminalCode(terminalCode)
                .prefix(entity.getPrefix())
                .rangeStart(entity.getRangeStart())
                .rangeEnd(entity.getRangeEnd())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
