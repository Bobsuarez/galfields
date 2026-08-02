package co.com.galfields.pos_transactions.jpa.sale.shadow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationShadowJpaRepository extends JpaRepository<LocationShadowEntity, Long> {
    Optional<LocationShadowEntity> findByName(String name);
}
