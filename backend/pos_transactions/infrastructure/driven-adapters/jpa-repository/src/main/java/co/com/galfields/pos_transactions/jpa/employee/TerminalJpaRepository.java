package co.com.galfields.pos_transactions.jpa.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalJpaRepository extends JpaRepository<TerminalEntity, Long> {
    Optional<TerminalEntity> findByTerminalCode(String terminalCode);

    List<TerminalEntity> findAllByOrderByTerminalCodeAsc();
}
