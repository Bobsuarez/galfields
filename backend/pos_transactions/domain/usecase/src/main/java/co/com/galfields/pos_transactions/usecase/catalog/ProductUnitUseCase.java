package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.InvalidStateException;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.ProductUnit;
import co.com.galfields.pos_transactions.model.catalog.ProductVariant;
import co.com.galfields.pos_transactions.model.catalog.gateways.LocationCatalogRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.ProductRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.ProductUnitRepository;
import co.com.galfields.pos_transactions.model.inventory.Inventory;
import co.com.galfields.pos_transactions.model.inventory.gateways.InventoryRepository;
import lombok.RequiredArgsConstructor;

/**
 * Mirrors backend/pos's ProductUnitService: no {@code isBase} on the
 * request — a unit created here is always non-base (the migration backfill
 * is the only base-unit source); the base unit's conversionFactor is
 * immutable at 1; deactivating the base unit is rejected.
 */
@RequiredArgsConstructor
public class ProductUnitUseCase {

    private static final String DEFAULT_LOCATION_NAME = "Bogotá - Chapinero";

    private final ProductUnitRepository productUnitRepository;
    private final ProductRepository productRepository;
    private final LocationCatalogRepository locationRepository;
    private final InventoryRepository inventoryRepository;

    public ProductUnit create(Long variantId, ProductUnit unit) {
        findVariantOrThrow(variantId);
        unit.setVariantId(variantId);
        unit.setBase(false);
        return withResolvedStock(productUnitRepository.save(unit));
    }

    public ProductUnit update(Long variantId, Long productUnitId, ProductUnit patch) {
        ProductUnit unit = findUnitOrThrow(variantId, productUnitId);

        if (unit.isBase() && !unit.getConversionFactor().equals(patch.getConversionFactor())) {
            throw new IllegalArgumentException("La unidad base siempre debe tener factor de conversión 1");
        }

        unit.setUnitName(patch.getUnitName());
        unit.setConversionFactor(patch.getConversionFactor());
        unit.setUnitPrice(patch.getUnitPrice());
        unit.setBarcode(patch.getBarcode());
        return withResolvedStock(productUnitRepository.save(unit));
    }

    public void deactivate(Long variantId, Long productUnitId) {
        ProductUnit unit = findUnitOrThrow(variantId, productUnitId);
        if (unit.isBase()) {
            throw new InvalidStateException("No se puede desactivar la unidad base de una variante");
        }
        unit.setActive(false);
        productUnitRepository.save(unit);
    }

    private void findVariantOrThrow(Long variantId) {
        productRepository.findVariantById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant " + variantId + " not found"));
    }

    private ProductUnit findUnitOrThrow(Long variantId, Long productUnitId) {
        return productUnitRepository.findByIdAndVariantId(productUnitId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product unit " + productUnitId + " not found for variant " + variantId));
    }

    private ProductUnit withResolvedStock(ProductUnit unit) {
        ProductVariant variant = productRepository.findVariantById(unit.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant " + unit.getVariantId() + " not found"));
        Long locationId = locationRepository.findByName(DEFAULT_LOCATION_NAME)
                .map(l -> l.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Default location '" + DEFAULT_LOCATION_NAME + "' not found"));
        int variantStock = inventoryRepository.findByVariantAndLocation(variant.getVariantId(), locationId)
                .map(Inventory::getQuantityOnHand)
                .orElse(0);
        unit.setStock(Math.floorDiv(variantStock, unit.getConversionFactor()));
        return unit;
    }
}
