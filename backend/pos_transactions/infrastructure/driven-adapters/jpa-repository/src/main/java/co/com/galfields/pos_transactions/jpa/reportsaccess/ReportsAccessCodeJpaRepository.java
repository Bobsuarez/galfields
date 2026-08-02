package co.com.galfields.pos_transactions.jpa.reportsaccess;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportsAccessCodeJpaRepository extends JpaRepository<ReportsAccessCodeEntity, Long> {
    Optional<ReportsAccessCodeEntity> findFirstByOrderByGeneratedAtDesc();
}
