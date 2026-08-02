package co.com.galfields.pos_transactions.jpa.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttachFileRefJpaRepository extends JpaRepository<AttachFileRefEntity, Long> {
    Optional<AttachFileRefEntity> findByName(String name);
}
