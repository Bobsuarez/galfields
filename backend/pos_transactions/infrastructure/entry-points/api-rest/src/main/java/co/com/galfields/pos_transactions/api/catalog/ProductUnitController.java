package co.com.galfields.pos_transactions.api.catalog;

import co.com.galfields.pos_transactions.model.catalog.ProductUnit;
import co.com.galfields.pos_transactions.usecase.catalog.ProductUnitUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors backend/pos's ProductUnitController 1:1. No isBase in the
 * request — a unit created here is always non-base; the base unit is
 * exclusively the migration backfill's job (see spec 03's Decisions). */
@RestController
@RequestMapping("/api/product-variants/{variantId}/units")
@RequiredArgsConstructor
public class ProductUnitController {

    private final ProductUnitUseCase productUnitUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductUnitResponse create(@PathVariable("variantId") Long variantId, @RequestBody @Valid ProductUnitRequest request) {
        return toResponse(productUnitUseCase.create(variantId, toDomain(request)));
    }

    @PutMapping("/{unitId}")
    @Transactional
    public ProductUnitResponse update(
            @PathVariable("variantId") Long variantId,
            @PathVariable("unitId") Long unitId,
            @RequestBody @Valid ProductUnitRequest request) {
        return toResponse(productUnitUseCase.update(variantId, unitId, toDomain(request)));
    }

    @DeleteMapping("/{unitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deactivate(@PathVariable("variantId") Long variantId, @PathVariable("unitId") Long unitId) {
        productUnitUseCase.deactivate(variantId, unitId);
    }

    private ProductUnit toDomain(ProductUnitRequest request) {
        return ProductUnit.builder()
                .unitName(request.unitName())
                .conversionFactor(request.conversionFactor())
                .unitPrice(request.unitPrice())
                .barcode(request.barcode())
                .active(true)
                .build();
    }

    private ProductUnitResponse toResponse(ProductUnit unit) {
        return new ProductUnitResponse(unit.getProductUnitId(), unit.getUnitName(), unit.getConversionFactor(),
                unit.getUnitPrice(), unit.getStock(), unit.getBarcode(), unit.isBase(), unit.isActive());
    }
}
