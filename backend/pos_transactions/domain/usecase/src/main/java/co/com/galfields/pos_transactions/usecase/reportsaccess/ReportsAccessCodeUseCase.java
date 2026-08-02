package co.com.galfields.pos_transactions.usecase.reportsaccess;

import co.com.galfields.pos_transactions.model.reportsaccess.ReportsAccessCode;
import co.com.galfields.pos_transactions.model.reportsaccess.gateways.ReportsAccessCodeRepository;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Mirrors backend/pos's ReportsAccessCodeService. Append-only: every
 * "Generar código" inserts a new row instead of updating one — the
 * currently valid code is always the most recently generated row, so a new
 * one implicitly invalidates the previous without deleting it (harmless
 * growing audit log).
 */
@RequiredArgsConstructor
public class ReportsAccessCodeUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReportsAccessCodeRepository reportsAccessCodeRepository;

    public ReportsAccessCode generate() {
        ReportsAccessCode code = ReportsAccessCode.builder()
                .code(String.format("%06d", RANDOM.nextInt(1_000_000)))
                .generatedAt(LocalDateTime.now())
                .build();
        return reportsAccessCodeRepository.save(code);
    }

    public boolean validate(String code) {
        return reportsAccessCodeRepository.findLatest()
                .map(latest -> latest.getCode().equals(code))
                .orElse(false);
    }
}
