package co.com.galfields.pos_transactions.model.catalog.gateways;

import co.com.galfields.pos_transactions.model.catalog.Location;

import java.util.List;
import java.util.Optional;

/** Full CRUD over locations — the real owner of the table (Fase 4). Distinct
 * from Sale's/Inventory's own private read-only LocationReferenceGateway
 * (Fases 2/3, built before this module existed) — see spec 04's Fase 2/3
 * coordination note; those keep working unchanged, this is not a
 * replacement for them. */
public interface LocationCatalogRepository {
    Optional<Location> findById(Long locationId);

    Optional<Location> findByName(String name);

    List<Location> findAllOrderByName();

    Location save(Location location);

    void deleteById(Long locationId);
}
