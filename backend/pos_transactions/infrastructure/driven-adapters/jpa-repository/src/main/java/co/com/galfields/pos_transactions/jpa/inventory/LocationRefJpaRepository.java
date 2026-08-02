package co.com.galfields.pos_transactions.jpa.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRefJpaRepository extends JpaRepository<LocationRefEntity, Long> {
    Optional<LocationRefEntity> findByName(String name);
}
