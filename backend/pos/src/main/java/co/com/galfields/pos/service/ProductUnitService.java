package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.ProductUnitRequest;
import co.com.galfields.pos.dto.ProductUnitResponse;
import co.com.galfields.pos.entity.Inventory;
import co.com.galfields.pos.entity.Location;
import co.com.galfields.pos.entity.ProductUnit;
import co.com.galfields.pos.entity.ProductVariant;
import co.com.galfields.pos.exception.InvalidStateException;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.InventoryRepository;
import co.com.galfields.pos.repository.LocationRepository;
import co.com.galfields.pos.repository.ProductUnitRepository;
import co.com.galfields.pos.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductUnitService {

    private static final String DEFAULT_LOCATION_NAME = "Bogotá - Chapinero";

    private final ProductUnitRepository productUnitRepository;
    private final ProductVariantRepository productVariantRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public ProductUnitResponse createUnit(Long variantId, ProductUnitRequest request) {
        ProductVariant variant = findVariantOrThrow(variantId);

        ProductUnit unit = new ProductUnit();
        unit.setVariant(variant);
        unit.setBase(false); // only the migration backfill creates a base unit - see V10__product_units.sql
        applyFields(unit, request);

        return toResponse(productUnitRepository.save(unit));
    }

    @Transactional
    public ProductUnitResponse updateUnit(Long variantId, Long productUnitId, ProductUnitRequest request) {
        ProductUnit unit = findUnitOrThrow(variantId, productUnitId);

        if (unit.isBase() && request.conversionFactor() != 1) {
            throw new IllegalArgumentException("La unidad base siempre debe tener factor de conversión 1");
        }

        applyFields(unit, request);
        return toResponse(productUnitRepository.save(unit));
    }

    @Transactional
    public void deactivateUnit(Long variantId, Long productUnitId) {
        ProductUnit unit = findUnitOrThrow(variantId, productUnitId);

        if (unit.isBase()) {
            throw new InvalidStateException("No se puede desactivar la unidad base de una variante");
        }

        unit.setActive(false);
        productUnitRepository.save(unit);
    }

    private ProductVariant findVariantOrThrow(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant " + variantId + " not found"));
    }

    private ProductUnit findUnitOrThrow(Long variantId, Long productUnitId) {
        return productUnitRepository.findByProductUnitIdAndVariant_VariantId(productUnitId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product unit " + productUnitId + " not found for variant " + variantId));
    }

    private void applyFields(ProductUnit unit, ProductUnitRequest request) {
        unit.setUnitName(request.unitName());
        unit.setConversionFactor(request.conversionFactor());
        unit.setUnitPrice(request.unitPrice());
        unit.setBarcode(request.barcode());
    }

    private ProductUnitResponse toResponse(ProductUnit unit) {
        return new ProductUnitResponse(
                unit.getProductUnitId(),
                unit.getUnitName(),
                unit.getConversionFactor(),
                unit.getUnitPrice(),
                Math.floorDiv(stockOf(unit.getVariant()), unit.getConversionFactor()),
                unit.getBarcode(),
                unit.isBase(),
                unit.isActive());
    }

    private int stockOf(ProductVariant variant) {
        Location location = locationRepository.findByName(DEFAULT_LOCATION_NAME)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Default location '" + DEFAULT_LOCATION_NAME + "' not found"));
        return inventoryRepository
                .findByVariant_VariantIdAndLocation_LocationId(variant.getVariantId(), location.getLocationId())
                .map(Inventory::getQuantityOnHand)
                .orElse(0);
    }
}
