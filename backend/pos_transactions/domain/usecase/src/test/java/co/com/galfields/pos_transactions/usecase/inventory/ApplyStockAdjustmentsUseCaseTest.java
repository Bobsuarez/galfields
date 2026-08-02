package co.com.galfields.pos_transactions.usecase.inventory;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.inventory.Inventory;
import co.com.galfields.pos_transactions.model.inventory.StockAdjustmentItem;
import co.com.galfields.pos_transactions.model.inventory.StockAdjustmentResult;
import co.com.galfields.pos_transactions.model.inventory.gateways.InventoryRepository;
import co.com.galfields.pos_transactions.model.inventory.gateways.LocationReferenceGateway;
import co.com.galfields.pos_transactions.model.inventory.gateways.ProductVariantReferenceGateway;
import co.com.galfields.pos_transactions.model.inventory.gateways.StockAdjustmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplyStockAdjustmentsUseCaseTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private StockAdjustmentRepository stockAdjustmentRepository;
    @Mock
    private LocationReferenceGateway locationReferenceGateway;
    @Mock
    private ProductVariantReferenceGateway productVariantReferenceGateway;

    private ApplyStockAdjustmentsUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ApplyStockAdjustmentsUseCase(inventoryRepository, stockAdjustmentRepository,
                locationReferenceGateway, productVariantReferenceGateway);
        when(locationReferenceGateway.findIdByName("Bogotá - Chapinero")).thenReturn(Optional.of(10L));
    }

    @Test
    void replaysAlreadyProcessedItemWithoutDoubleApplying() {
        when(stockAdjustmentRepository.existsByClientEventIdAndVariant("evt-1", 12L)).thenReturn(true);
        Inventory current = Inventory.builder().quantityOnHand(5).build();
        when(inventoryRepository.findByVariantAndLocation(12L, 10L)).thenReturn(Optional.of(current));

        List<StockAdjustmentResult> results = useCase.execute("evt-1", List.of(new StockAdjustmentItem(12L, -2)));

        assertThat(results).containsExactly(new StockAdjustmentResult(12L, true, 5));
        verify(inventoryRepository, never()).save(any());
        verify(stockAdjustmentRepository, never()).save(any());
    }

    @Test
    void createsInventoryRowOnFirstAdjustmentAndAllowsNegativeStock() {
        when(stockAdjustmentRepository.existsByClientEventIdAndVariant("evt-2", 12L)).thenReturn(false);
        when(productVariantReferenceGateway.existsById(12L)).thenReturn(true);
        when(inventoryRepository.findByVariantAndLocation(12L, 10L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<StockAdjustmentResult> results = useCase.execute("evt-2", List.of(new StockAdjustmentItem(12L, -5)));

        assertThat(results).containsExactly(new StockAdjustmentResult(12L, false, -5));

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantityOnHand()).isEqualTo(-5);
        assertThat(captor.getValue().getLocationId()).isEqualTo(10L);

        verify(stockAdjustmentRepository).save(any());
    }

    @Test
    void addsDeltaToExistingInventory() {
        when(stockAdjustmentRepository.existsByClientEventIdAndVariant("evt-3", 12L)).thenReturn(false);
        when(productVariantReferenceGateway.existsById(12L)).thenReturn(true);
        Inventory existing = Inventory.builder().quantityOnHand(10).build();
        when(inventoryRepository.findByVariantAndLocation(12L, 10L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<StockAdjustmentResult> results = useCase.execute("evt-3", List.of(new StockAdjustmentItem(12L, 3)));

        assertThat(results).containsExactly(new StockAdjustmentResult(12L, false, 13));
    }

    @Test
    void throwsNotFoundWhenVariantMissing() {
        when(stockAdjustmentRepository.existsByClientEventIdAndVariant("evt-4", 999L)).thenReturn(false);
        when(productVariantReferenceGateway.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("evt-4", List.of(new StockAdjustmentItem(999L, -1))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenDefaultLocationMissing() {
        when(locationReferenceGateway.findIdByName("Bogotá - Chapinero")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("evt-5", List.of(new StockAdjustmentItem(12L, -1))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void appliesMultipleItemsInOneBatch() {
        when(stockAdjustmentRepository.existsByClientEventIdAndVariant("evt-6", 1L)).thenReturn(false);
        when(stockAdjustmentRepository.existsByClientEventIdAndVariant("evt-6", 2L)).thenReturn(false);
        when(productVariantReferenceGateway.existsById(1L)).thenReturn(true);
        when(productVariantReferenceGateway.existsById(2L)).thenReturn(true);
        when(inventoryRepository.findByVariantAndLocation(any(), any())).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<StockAdjustmentResult> results = useCase.execute("evt-6",
                List.of(new StockAdjustmentItem(1L, -2), new StockAdjustmentItem(2L, -1)));

        assertThat(results).containsExactly(
                new StockAdjustmentResult(1L, false, -2),
                new StockAdjustmentResult(2L, false, -1));
    }
}
