package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.BrandRequest;
import co.com.galfields.pos.dto.BrandResponse;
import co.com.galfields.pos.entity.Brand;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.BrandRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        Brand brand = new Brand();
        applyFields(brand, request);
        return toResponse(brandRepository.save(brand));
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrand(Long brandId) {
        return toResponse(findOrThrow(brandId));
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands() {
        return brandRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BrandResponse updateBrand(Long brandId, BrandRequest request) {
        Brand brand = findOrThrow(brandId);
        applyFields(brand, request);
        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public void deleteBrand(Long brandId) {
        Brand brand = findOrThrow(brandId);
        brandRepository.delete(brand);
    }

    private Brand findOrThrow(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand " + brandId + " not found"));
    }

    private void applyFields(Brand brand, BrandRequest request) {
        brand.setName(request.name());
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getBrandId(),
                brand.getName(),
                brand.getCreatedAt());
    }
}
