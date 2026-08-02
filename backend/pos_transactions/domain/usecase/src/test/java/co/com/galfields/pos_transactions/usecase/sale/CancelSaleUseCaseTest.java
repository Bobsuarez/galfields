package co.com.galfields.pos_transactions.usecase.sale;

import co.com.galfields.pos_transactions.model.InvalidStateException;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.sale.Sale;
import co.com.galfields.pos_transactions.model.sale.SaleItem;
import co.com.galfields.pos_transactions.model.sale.StockDelta;
import co.com.galfields.pos_transactions.model.sale.gateways.SaleRepository;
import co.com.galfields.pos_transactions.model.sale.gateways.StockGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelSaleUseCaseTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private StockGateway stockGateway;

    private CancelSaleUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new CancelSaleUseCase(saleRepository, stockGateway);
    }

    @Test
    void cancelByTransactionIdReversesStockAndMarksCancelled() {
        SaleItem item = SaleItem.builder().variantId(12L).quantity(2).conversionFactor(20).build();
        Sale sale = Sale.builder().transactionId(1L).clientEventId("evt-1").locationId(10L)
                .items(List.of(item)).build();
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(saleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelByTransactionId(1L);

        verify(stockGateway).applyAdjustments(eq("cancel-evt-1"), eq(10L), eq(List.of(new StockDelta(12L, 40))));
        assertThat(sale.getCancelledAt()).isNotNull();
        verify(saleRepository).save(sale);
    }

    @Test
    void cancelByClientEventIdResolvesTransactionFirst() {
        Sale sale = Sale.builder().transactionId(2L).clientEventId("evt-2").locationId(10L).items(List.of()).build();
        when(saleRepository.findByClientEventId("evt-2")).thenReturn(Optional.of(sale));
        when(saleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelByClientEventId("evt-2");

        assertThat(sale.getCancelledAt()).isNotNull();
        verify(stockGateway).applyAdjustments(eq("cancel-evt-2"), eq(10L), eq(List.of()));
    }

    @Test
    void throwsInvalidStateWhenAlreadyCancelled() {
        Sale sale = Sale.builder().transactionId(1L).clientEventId("evt-1")
                .cancelledAt(LocalDateTime.now()).items(List.of()).build();
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> useCase.cancelByTransactionId(1L))
                .isInstanceOf(InvalidStateException.class);
        verify(stockGateway, org.mockito.Mockito.never()).applyAdjustments(any(), any(), any());
    }

    @Test
    void throwsNotFoundWhenTransactionUnknown() {
        when(saleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.cancelByTransactionId(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
