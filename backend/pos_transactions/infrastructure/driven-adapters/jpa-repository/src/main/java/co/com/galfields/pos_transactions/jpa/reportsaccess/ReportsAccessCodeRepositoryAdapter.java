package co.com.galfields.pos_transactions.jpa.reportsaccess;

import co.com.galfields.pos_transactions.model.reportsaccess.ReportsAccessCode;
import co.com.galfields.pos_transactions.model.reportsaccess.gateways.ReportsAccessCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportsAccessCodeRepositoryAdapter implements ReportsAccessCodeRepository {

    private final ReportsAccessCodeJpaRepository repository;

    @Override
    public ReportsAccessCode save(ReportsAccessCode code) {
        ReportsAccessCodeEntity entity = new ReportsAccessCodeEntity();
        entity.setReportsAccessCodeId(code.getReportsAccessCodeId());
        entity.setCode(code.getCode());
        entity.setGeneratedAt(code.getGeneratedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<ReportsAccessCode> findLatest() {
        return repository.findFirstByOrderByGeneratedAtDesc().map(this::toDomain);
    }

    private ReportsAccessCode toDomain(ReportsAccessCodeEntity entity) {
        return ReportsAccessCode.builder()
                .reportsAccessCodeId(entity.getReportsAccessCodeId())
                .code(entity.getCode())
                .generatedAt(entity.getGeneratedAt())
                .build();
    }
}
