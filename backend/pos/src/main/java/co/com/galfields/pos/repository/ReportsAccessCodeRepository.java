package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.ReportsAccessCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportsAccessCodeRepository extends JpaRepository<ReportsAccessCode, Long> {

    Optional<ReportsAccessCode> findFirstByOrderByGeneratedAtDesc();
}
