package co.com.galfields.pos_transactions.jpa.catalog;

import co.com.galfields.pos_transactions.model.catalog.Brand;
import co.com.galfields.pos_transactions.model.catalog.gateways.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryAdapter implements BrandRepository {

    private final BrandJpaRepository repository;

    @Override
    public Optional<Brand> findById(Long brandId) {
        return repository.findById(brandId).map(this::toDomain);
    }

    @Override
    public List<Brand> findAllOrderByName() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public Brand save(Brand brand) {
        BrandEntity entity = new BrandEntity();
        entity.setBrandId(brand.getBrandId());
        entity.setName(brand.getName());
        // See CategoryRepositoryAdapter's comment on createdAt.
        entity.setCreatedAt(brand.getCreatedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long brandId) {
        repository.deleteById(brandId);
    }

    private Brand toDomain(BrandEntity entity) {
        return Brand.builder()
                .brandId(entity.getBrandId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
