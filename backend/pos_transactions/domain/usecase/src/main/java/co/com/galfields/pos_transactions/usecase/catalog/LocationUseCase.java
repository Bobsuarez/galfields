package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.Location;
import co.com.galfields.pos_transactions.model.catalog.gateways.LocationCatalogRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Mirrors backend/pos's LocationService — the real, full-CRUD owner of
 * locations (Fase 4). Sale/Inventory (Fases 2/3) keep their own separate
 * read-only LocationReferenceGateway shadow, unaffected by this. */
@RequiredArgsConstructor
public class LocationUseCase {

    private final LocationCatalogRepository locationRepository;

    public Location create(Location location) {
        return locationRepository.save(location);
    }

    public Location get(Long locationId) {
        return findOrThrow(locationId);
    }

    public List<Location> list() {
        return locationRepository.findAllOrderByName();
    }

    public Location update(Long locationId, Location patch) {
        Location existing = findOrThrow(locationId);
        existing.setName(patch.getName());
        existing.setAddress(patch.getAddress());
        existing.setPhone(patch.getPhone());
        return locationRepository.save(existing);
    }

    public void delete(Long locationId) {
        findOrThrow(locationId);
        locationRepository.deleteById(locationId);
    }

    private Location findOrThrow(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location " + locationId + " not found"));
    }
}
