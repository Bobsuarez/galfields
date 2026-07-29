package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.ProductUnitRequest;
import co.com.galfields.pos.dto.ProductUnitResponse;
import co.com.galfields.pos.service.ProductUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-variants/{variantId}/units")
@RequiredArgsConstructor
public class ProductUnitController {

    private final ProductUnitService productUnitService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductUnitResponse create(
            @PathVariable Long variantId,
            @RequestBody @Valid ProductUnitRequest request
    ) {
        return productUnitService.createUnit(variantId, request);
    }

    @PutMapping("/{unitId}")
    public ProductUnitResponse update(
            @PathVariable Long variantId,
            @PathVariable Long unitId,
            @RequestBody @Valid ProductUnitRequest request
    ) {
        return productUnitService.updateUnit(variantId, unitId, request);
    }

    @DeleteMapping("/{unitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long variantId, @PathVariable Long unitId) {
        productUnitService.deactivateUnit(variantId, unitId);
    }
}
