package co.com.galfields.pos_transactions.jpa.inventory;

import co.com.galfields.pos_transactions.model.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InventoryRepositoryAdapterTest {

    @Mock
    private InventoryJpaRepository repository;

    private InventoryRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new InventoryRepositoryAdapter(repository);
    }

    @Test
    void findByVariantAndLocationMapsToDomain() {
        InventoryEntity entity = new InventoryEntity();
        entity.setInventoryId(1L);
        entity.setVariantId(12L);
        entity.setLocationId(10L);
        entity.setQuantityOnHand(5);
        when(repository.findByVariantIdAndLocationId(12L, 10L)).thenReturn(Optional.of(entity));

        Optional<Inventory> result = adapter.findByVariantAndLocation(12L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().getQuantityOnHand()).isEqualTo(5);
    }

    @Test
    void saveMapsDomainToEntityAndBack() {
        when(repository.save(any())).thenAnswer(invocation -> {
            InventoryEntity entity = invocation.getArgument(0);
            entity.setInventoryId(99L);
            return entity;
        });

        Inventory saved = adapter.save(Inventory.builder().variantId(12L).locationId(10L).quantityOnHand(-3).build());

        assertThat(saved.getInventoryId()).isEqualTo(99L);
        assertThat(saved.getQuantityOnHand()).isEqualTo(-3);
    }
}
