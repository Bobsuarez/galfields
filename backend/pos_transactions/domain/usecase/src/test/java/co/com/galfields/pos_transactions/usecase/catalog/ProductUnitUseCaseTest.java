package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.InvalidStateException;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.Location;
import co.com.galfields.pos_transactions.model.catalog.ProductUnit;
import co.com.galfields.pos_transactions.model.catalog.ProductVariant;
import co.com.galfields.pos_transactions.model.catalog.gateways.LocationCatalogRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.ProductRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.ProductUnitRepository;
import co.com.galfields.pos_transactions.model.inventory.Inventory;
import co.com.galfields.pos_transactions.model.inventory.gateways.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ProductUnitUseCaseTest {

    @Mock
    private ProductUnitRepository productUnitRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private LocationCatalogRepository locationRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    private ProductUnitUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ProductUnitUseCase(productUnitRepository, productRepository, locationRepository, inventoryRepository);

        when(productRepository.findVariantById(1L)).thenReturn(Optional.of(ProductVariant.builder().variantId(1L).build()));
        when(locationRepository.findByName("Bogotá - Chapinero"))
                .thenReturn(Optional.of(Location.builder().locationId(10L).build()));
        when(inventoryRepository.findByVariantAndLocation(1L, 10L))
                .thenReturn(Optional.of(Inventory.builder().quantityOnHand(47).build()));
    }

    @Test
    void createAlwaysForcesNonBaseAndResolvesStock() {
        ProductUnit input = ProductUnit.builder().unitName("Media").conversionFactor(20).unitPrice(BigDecimal.TEN).base(true).build();
        when(productUnitRepository.save(any())).thenAnswer(inv -> {
            ProductUnit u = inv.getArgument(0);
            u.setProductUnitId(5L);
            return u;
        });

        ProductUnit result = useCase.create(1L, input);

        assertThat(result.isBase()).isFalse();
        assertThat(result.getStock()).isEqualTo(2); // floorDiv(47, 20)
    }

    @Test
    void createThrowsNotFoundWhenVariantMissing() {
        when(productRepository.findVariantById(999L)).thenReturn(Optional.empty());
        ProductUnit input = ProductUnit.builder().unitName("X").conversionFactor(1).unitPrice(BigDecimal.ONE).build();

        assertThatThrownBy(() -> useCase.create(999L, input)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsChangingBaseUnitConversionFactor() {
        ProductUnit baseUnit = ProductUnit.builder().productUnitId(1L).variantId(1L).unitName("Unidad")
                .conversionFactor(1).unitPrice(BigDecimal.ONE).base(true).active(true).build();
        when(productUnitRepository.findByIdAndVariantId(1L, 1L)).thenReturn(Optional.of(baseUnit));

        ProductUnit patch = ProductUnit.builder().unitName("Unidad").conversionFactor(2).unitPrice(BigDecimal.ONE).build();

        assertThatThrownBy(() -> useCase.update(1L, 1L, patch)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivateRejectsBaseUnit() {
        ProductUnit baseUnit = ProductUnit.builder().productUnitId(1L).variantId(1L).base(true).active(true).build();
        when(productUnitRepository.findByIdAndVariantId(1L, 1L)).thenReturn(Optional.of(baseUnit));

        assertThatThrownBy(() -> useCase.deactivate(1L, 1L)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void deactivateNonBaseUnitFlipsActiveFalse() {
        ProductUnit unit = ProductUnit.builder().productUnitId(2L).variantId(1L).base(false).active(true).build();
        when(productUnitRepository.findByIdAndVariantId(2L, 1L)).thenReturn(Optional.of(unit));
        when(productUnitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.deactivate(1L, 2L);

        assertThat(unit.isActive()).isFalse();
    }
}
