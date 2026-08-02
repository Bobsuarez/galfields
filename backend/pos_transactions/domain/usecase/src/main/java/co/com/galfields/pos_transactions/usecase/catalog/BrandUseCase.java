package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.Brand;
import co.com.galfields.pos_transactions.model.catalog.gateways.BrandRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Mirrors backend/pos's BrandService. */
@RequiredArgsConstructor
public class BrandUseCase {

    private final BrandRepository brandRepository;

    public Brand create(Brand brand) {
        return brandRepository.save(brand);
    }

    public Brand get(Long brandId) {
        return findOrThrow(brandId);
    }

    public List<Brand> list() {
        return brandRepository.findAllOrderByName();
    }

    public Brand update(Long brandId, Brand patch) {
        Brand existing = findOrThrow(brandId);
        existing.setName(patch.getName());
        return brandRepository.save(existing);
    }

    public void delete(Long brandId) {
        findOrThrow(brandId);
        brandRepository.deleteById(brandId);
    }

    private Brand findOrThrow(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand " + brandId + " not found"));
    }
}
