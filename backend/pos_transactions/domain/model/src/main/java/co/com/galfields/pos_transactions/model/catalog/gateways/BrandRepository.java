package co.com.galfields.pos_transactions.model.catalog.gateways;

import co.com.galfields.pos_transactions.model.catalog.Brand;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {
    Optional<Brand> findById(Long brandId);

    List<Brand> findAllOrderByName();

    Brand save(Brand brand);

    void deleteById(Long brandId);
}
