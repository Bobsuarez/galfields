package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.Terminal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {

    List<Terminal> findAllByOrderByTerminalCodeAsc();

    Optional<Terminal> findByTerminalCode(String terminalCode);
}
