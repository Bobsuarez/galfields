package co.com.galfields.pos_transactions.api.catalog;

import co.com.galfields.pos_transactions.model.catalog.Brand;
import co.com.galfields.pos_transactions.usecase.catalog.BrandUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Mirrors backend/pos's BrandController 1:1. */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandUseCase brandUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public BrandResponse create(@RequestBody @Valid BrandRequest request) {
        return toResponse(brandUseCase.create(Brand.builder().name(request.name()).build()));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<BrandResponse> list() {
        return brandUseCase.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{brandId}")
    @Transactional(readOnly = true)
    public BrandResponse get(@PathVariable("brandId") Long brandId) {
        return toResponse(brandUseCase.get(brandId));
    }

    @PutMapping("/{brandId}")
    @Transactional
    public BrandResponse update(@PathVariable("brandId") Long brandId, @RequestBody @Valid BrandRequest request) {
        return toResponse(brandUseCase.update(brandId, Brand.builder().name(request.name()).build()));
    }

    @DeleteMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("brandId") Long brandId) {
        brandUseCase.delete(brandId);
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getBrandId(), brand.getName(), brand.getCreatedAt());
    }
}
