package co.com.galfields.pos_transactions.jpa.sale.shadow;

import co.com.galfields.pos_transactions.model.sale.StockAdjustmentOutcome;
import co.com.galfields.pos_transactions.model.sale.StockDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockGatewayAdapterTest {

    @Mock
    private InventoryShadowJpaRepository inventoryRepository;
    @Mock
    private StockAdjustmentShadowJpaRepository stockAdjustmentRepository;

    private StockGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new StockGatewayAdapter(inventoryRepository, stockAdjustmentRepository);
    }

    @Test
    void replaysAlreadyProcessedAdjustmentWithoutDoubleApplying() {
        when(stockAdjustmentRepository.existsByClientEventIdAndVariantId("evt-1", 12L)).thenReturn(true);
        InventoryShadowEntity inventory = new InventoryShadowEntity();
        inventory.setQuantityOnHand(38);
        when(inventoryRepository.findByVariantIdAndLocationId(12L, 10L)).thenReturn(Optional.of(inventory));

        List<StockAdjustmentOutcome> outcomes = adapter.applyAdjustments("evt-1", 10L, List.of(new StockDelta(12L, -2)));

        assertThat(outcomes).containsExactly(new StockAdjustmentOutcome(12L, true, 38));
        verify(inventoryRepository, never()).save(any());
        verify(stockAdjustmentRepository, never()).save(any());
    }

    @Test
    void createsInventoryRowOnFirstAdjustmentAndAllowsNegativeStock() {
        when(stockAdjustmentRepository.existsByClientEventIdAndVariantId("evt-2", 12L)).thenReturn(false);
        when(inventoryRepository.findByVariantIdAndLocationId(12L, 10L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<StockAdjustmentOutcome> outcomes = adapter.applyAdjustments("evt-2", 10L, List.of(new StockDelta(12L, -5)));

        assertThat(outcomes).containsExactly(new StockAdjustmentOutcome(12L, false, -5));

        ArgumentCaptor<InventoryShadowEntity> captor = ArgumentCaptor.forClass(InventoryShadowEntity.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantityOnHand()).isEqualTo(-5);

        ArgumentCaptor<StockAdjustmentShadowEntity> adjustmentCaptor = ArgumentCaptor.forClass(StockAdjustmentShadowEntity.class);
        verify(stockAdjustmentRepository).save(adjustmentCaptor.capture());
        assertThat(adjustmentCaptor.getValue().getClientEventId()).isEqualTo("evt-2");
        assertThat(adjustmentCaptor.getValue().getQuantityDelta()).isEqualTo(-5);
        assertThat(adjustmentCaptor.getValue().getResultingQuantity()).isEqualTo(-5);
    }

    @Test
    void addsDeltaToExistingInventory() {
        when(stockAdjustmentRepository.existsByClientEventIdAndVariantId("evt-3", 12L)).thenReturn(false);
        InventoryShadowEntity existing = new InventoryShadowEntity();
        existing.setQuantityOnHand(10);
        when(inventoryRepository.findByVariantIdAndLocationId(12L, 10L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<StockAdjustmentOutcome> outcomes = adapter.applyAdjustments("evt-3", 10L, List.of(new StockDelta(12L, 3)));

        assertThat(outcomes).containsExactly(new StockAdjustmentOutcome(12L, false, 13));
    }
}
