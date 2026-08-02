package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationCatalogJpaRepository extends JpaRepository<LocationCatalogEntity, Long> {
    Optional<LocationCatalogEntity> findByName(String name);

    List<LocationCatalogEntity> findAllByOrderByNameAsc();
}
