package co.com.galfields.pos_transactions.jpa.sale;

import co.com.galfields.pos_transactions.model.sale.Payment;
import co.com.galfields.pos_transactions.model.sale.PaymentStatus;
import co.com.galfields.pos_transactions.model.sale.Sale;
import co.com.galfields.pos_transactions.model.sale.SaleItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SaleRepositoryAdapterTest {

    @Mock
    private SalesTransactionJpaRepository transactionRepository;
    @Mock
    private SaleItemJpaRepository itemRepository;
    @Mock
    private PaymentJpaRepository paymentRepository;

    private SaleRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new SaleRepositoryAdapter(transactionRepository, itemRepository, paymentRepository);
    }

    @Test
    void saveInsertsTransactionThenItemsAndPaymentsUnderItsId() {
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            SalesTransactionEntity entity = invocation.getArgument(0);
            entity.setTransactionId(99L);
            return entity;
        });
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Sale sale = Sale.builder()
                .clientEventId("evt-1")
                .employeeId(20L)
                .locationId(10L)
                .totalAmount(new BigDecimal("9000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .paymentStatus(PaymentStatus.Paid)
                .items(List.of(SaleItem.builder().variantId(12L).quantity(2)
                        .unitPrice(new BigDecimal("4500")).subtotal(new BigDecimal("9000"))
                        .unitName("Unidad").conversionFactor(1).discountPerItem(BigDecimal.ZERO).build()))
                .payments(List.of(Payment.builder().paymentMethodId(3L).amount(new BigDecimal("9000")).build()))
                .build();

        Sale saved = adapter.save(sale);

        assertThat(saved.getTransactionId()).isEqualTo(99L);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().getFirst().getVariantId()).isEqualTo(12L);
        assertThat(saved.getPayments()).hasSize(1);
        assertThat(saved.getPayments().getFirst().getPaymentMethodId()).isEqualTo(3L);
    }

    @Test
    void findByClientEventIdAssemblesFullAggregate() {
        SalesTransactionEntity transaction = new SalesTransactionEntity();
        transaction.setTransactionId(1L);
        transaction.setClientEventId("evt-2");
        transaction.setPaymentStatus(PaymentStatus.Paid);
        when(transactionRepository.findByClientEventId("evt-2")).thenReturn(Optional.of(transaction));

        SaleItemEntity itemEntity = new SaleItemEntity();
        itemEntity.setSaleItemId(5L);
        itemEntity.setVariantId(12L);
        when(itemRepository.findByTransactionId(1L)).thenReturn(List.of(itemEntity));

        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setPaymentId(7L);
        paymentEntity.setPaymentMethodId(3L);
        when(paymentRepository.findByTransactionId(1L)).thenReturn(List.of(paymentEntity));

        Optional<Sale> result = adapter.findByClientEventId("evt-2");

        assertThat(result).isPresent();
        assertThat(result.get().getTransactionId()).isEqualTo(1L);
        assertThat(result.get().getItems()).extracting(SaleItem::getSaleItemId).containsExactly(5L);
        assertThat(result.get().getPayments()).extracting(Payment::getPaymentId).containsExactly(7L);
    }

    @Test
    void findByIdReturnsEmptyWhenUnknown() {
        when(transactionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(404L)).isEmpty();
    }
}
