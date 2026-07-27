package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.ReportsAccessCodeResponse;
import co.com.galfields.pos.entity.ReportsAccessCode;
import co.com.galfields.pos.repository.ReportsAccessCodeRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportsAccessCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReportsAccessCodeRepository reportsAccessCodeRepository;

    @Transactional
    public ReportsAccessCodeResponse generate() {
        ReportsAccessCode entity = new ReportsAccessCode();
        entity.setCode(String.format("%06d", RANDOM.nextInt(1_000_000)));
        entity.setGeneratedAt(LocalDateTime.now());
        ReportsAccessCode saved = reportsAccessCodeRepository.save(entity);
        return new ReportsAccessCodeResponse(saved.getCode(), saved.getGeneratedAt());
    }

    @Transactional(readOnly = true)
    public boolean validate(String code) {
        return reportsAccessCodeRepository.findFirstByOrderByGeneratedAtDesc()
                .map(latest -> latest.getCode().equals(code))
                .orElse(false);
    }
}
