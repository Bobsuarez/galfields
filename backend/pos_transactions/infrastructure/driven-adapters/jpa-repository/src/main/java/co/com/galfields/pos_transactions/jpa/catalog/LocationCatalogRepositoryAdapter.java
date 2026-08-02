package co.com.galfields.pos_transactions.jpa.catalog;

import co.com.galfields.pos_transactions.model.catalog.Location;
import co.com.galfields.pos_transactions.model.catalog.gateways.LocationCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LocationCatalogRepositoryAdapter implements LocationCatalogRepository {

    private final LocationCatalogJpaRepository repository;

    @Override
    public Optional<Location> findById(Long locationId) {
        return repository.findById(locationId).map(this::toDomain);
    }

    @Override
    public Optional<Location> findByName(String name) {
        return repository.findByName(name).map(this::toDomain);
    }

    @Override
    public List<Location> findAllOrderByName() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public Location save(Location location) {
        LocationCatalogEntity entity = new LocationCatalogEntity();
        entity.setLocationId(location.getLocationId());
        entity.setName(location.getName());
        entity.setAddress(location.getAddress());
        entity.setPhone(location.getPhone());
        // See CategoryRepositoryAdapter's comment on createdAt.
        entity.setCreatedAt(location.getCreatedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long locationId) {
        repository.deleteById(locationId);
    }

    private Location toDomain(LocationCatalogEntity entity) {
        return Location.builder()
                .locationId(entity.getLocationId())
                .name(entity.getName())
                .address(entity.getAddress())
                .phone(entity.getPhone())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
