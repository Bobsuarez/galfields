package co.com.galfields.pos_transactions.usecase.sale;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.sale.PaymentStatus;
import co.com.galfields.pos_transactions.model.sale.ProductUnitReference;
import co.com.galfields.pos_transactions.model.sale.Sale;
import co.com.galfields.pos_transactions.model.sale.SaleItem;
import co.com.galfields.pos_transactions.model.sale.gateways.EmployeeReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.LocationReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.PaymentMethodReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.ProductUnitReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.ProductVariantReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.SaleRepository;
import co.com.galfields.pos_transactions.model.sale.gateways.StockGateway;
import co.com.galfields.pos_transactions.model.sale.Payment;
import co.com.galfields.pos_transactions.model.sale.StockDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordSaleUseCaseTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private EmployeeReferenceGateway employeeReferenceGateway;
    @Mock
    private LocationReferenceGateway locationReferenceGateway;
    @Mock
    private ProductVariantReferenceGateway productVariantReferenceGateway;
    @Mock
    private ProductUnitReferenceGateway productUnitReferenceGateway;
    @Mock
    private PaymentMethodReferenceGateway paymentMethodReferenceGateway;
    @Mock
    private StockGateway stockGateway;

    private RecordSaleUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new RecordSaleUseCase(saleRepository, employeeReferenceGateway, locationReferenceGateway,
                productVariantReferenceGateway, productUnitReferenceGateway, paymentMethodReferenceGateway, stockGateway);
    }

    @Test
    void returnsExistingTransactionWhenClientEventIdAlreadyProcessed() {
        Sale existing = Sale.builder().transactionId(1L).clientEventId("evt-1").build();
        when(saleRepository.findByClientEventId("evt-1")).thenReturn(Optional.of(existing));

        Sale request = Sale.builder().clientEventId("evt-1").items(List.of()).payments(List.of()).build();
        RecordSaleResult result = useCase.execute(request);

        assertThat(result.alreadyProcessed()).isTrue();
        assertThat(result.sale()).isSameAs(existing);
        verify(saleRepository, never()).save(any());
        verify(stockGateway, never()).applyAdjustments(any(), any(), anyList());
    }

    @Test
    void recordsSaleAndAppliesStockDecrementAtBaseUnits() {
        when(saleRepository.findByClientEventId("evt-2")).thenReturn(Optional.empty());
        when(locationReferenceGateway.findIdByName("Bogotá - Chapinero")).thenReturn(Optional.of(10L));
        when(employeeReferenceGateway.findIdByUsername("pos-terminal")).thenReturn(Optional.of(20L));
        when(productVariantReferenceGateway.existsById(12L)).thenReturn(true);
        when(productUnitReferenceGateway.find(45L, 12L))
                .thenReturn(Optional.of(new ProductUnitReference("Media", 20)));
        when(paymentMethodReferenceGateway.existsById(3L)).thenReturn(true);

        Sale saved = Sale.builder().transactionId(99L).clientEventId("evt-2").build();
        when(saleRepository.save(any())).thenReturn(saved);

        SaleItem item = SaleItem.builder().variantId(12L).productUnitId(45L).quantity(2)
                .unitPrice(new BigDecimal("4500")).subtotal(new BigDecimal("9000")).build();
        Payment payment = Payment.builder().paymentMethodId(3L).amount(new BigDecimal("9000")).build();
        Sale request = Sale.builder().clientEventId("evt-2")
                .discountAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("9000"))
                .items(List.of(item)).payments(List.of(payment)).build();

        RecordSaleResult result = useCase.execute(request);

        assertThat(result.alreadyProcessed()).isFalse();
        assertThat(result.sale()).isSameAs(saved);
        assertThat(request.getEmployeeId()).isEqualTo(20L);
        assertThat(request.getLocationId()).isEqualTo(10L);
        assertThat(request.getTaxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(request.getPaymentStatus()).isEqualTo(PaymentStatus.Paid);
        assertThat(item.getUnitName()).isEqualTo("Media");
        assertThat(item.getConversionFactor()).isEqualTo(20);

        // 2 units of a "Media" (factor 20) -> -40 base units decremented.
        verify(stockGateway).applyAdjustments(eq("evt-2"), eq(10L), eq(List.of(new StockDelta(12L, -40))));
    }

    @Test
    void sellsAtBaseUnitWhenNoProductUnitIdGiven() {
        when(saleRepository.findByClientEventId("evt-3")).thenReturn(Optional.empty());
        when(locationReferenceGateway.findIdByName("Bogotá - Chapinero")).thenReturn(Optional.of(10L));
        when(employeeReferenceGateway.findIdByUsername("pos-terminal")).thenReturn(Optional.of(20L));
        when(productVariantReferenceGateway.existsById(12L)).thenReturn(true);
        when(paymentMethodReferenceGateway.existsById(3L)).thenReturn(true);
        when(saleRepository.save(any())).thenReturn(Sale.builder().transactionId(1L).build());

        SaleItem item = SaleItem.builder().variantId(12L).quantity(3)
                .unitPrice(BigDecimal.TEN).subtotal(new BigDecimal("30")).build();
        Payment payment = Payment.builder().paymentMethodId(3L).amount(new BigDecimal("30")).build();
        Sale request = Sale.builder().clientEventId("evt-3")
                .items(List.of(item)).payments(List.of(payment)).build();

        useCase.execute(request);

        assertThat(item.getUnitName()).isEqualTo("Unidad");
        assertThat(item.getConversionFactor()).isEqualTo(1);
        verify(stockGateway).applyAdjustments(eq("evt-3"), eq(10L), eq(List.of(new StockDelta(12L, -3))));
    }

    @Test
    void throwsNotFoundWhenVariantMissing() {
        when(saleRepository.findByClientEventId("evt-4")).thenReturn(Optional.empty());
        when(locationReferenceGateway.findIdByName("Bogotá - Chapinero")).thenReturn(Optional.of(10L));
        when(employeeReferenceGateway.findIdByUsername("pos-terminal")).thenReturn(Optional.of(20L));
        when(productVariantReferenceGateway.existsById(999L)).thenReturn(false);

        SaleItem item = SaleItem.builder().variantId(999L).quantity(1)
                .unitPrice(BigDecimal.ONE).subtotal(BigDecimal.ONE).build();
        Sale request = Sale.builder().clientEventId("evt-4").items(List.of(item)).payments(List.of()).build();

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
        verify(saleRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenPaymentMethodMissing() {
        when(saleRepository.findByClientEventId("evt-5")).thenReturn(Optional.empty());
        when(locationReferenceGateway.findIdByName("Bogotá - Chapinero")).thenReturn(Optional.of(10L));
        when(employeeReferenceGateway.findIdByUsername("pos-terminal")).thenReturn(Optional.of(20L));
        when(productVariantReferenceGateway.existsById(12L)).thenReturn(true);
        when(paymentMethodReferenceGateway.existsById(404L)).thenReturn(false);

        SaleItem item = SaleItem.builder().variantId(12L).quantity(1)
                .unitPrice(BigDecimal.ONE).subtotal(BigDecimal.ONE).build();
        Payment payment = Payment.builder().paymentMethodId(404L).amount(BigDecimal.ONE).build();
        Sale request = Sale.builder().clientEventId("evt-5").items(List.of(item)).payments(List.of(payment)).build();

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
        verify(saleRepository, never()).save(any());
    }
}
