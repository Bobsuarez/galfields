package co.com.galfields.pos_transactions.jpa.sale;

import co.com.galfields.pos_transactions.model.sale.Payment;
import co.com.galfields.pos_transactions.model.sale.Sale;
import co.com.galfields.pos_transactions.model.sale.SaleItem;
import co.com.galfields.pos_transactions.model.sale.gateways.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SaleRepositoryAdapter implements SaleRepository {

    private final SalesTransactionJpaRepository transactionRepository;
    private final SaleItemJpaRepository itemRepository;
    private final PaymentJpaRepository paymentRepository;

    @Override
    public Optional<Sale> findByClientEventId(String clientEventId) {
        return transactionRepository.findByClientEventId(clientEventId).map(this::toDomain);
    }

    @Override
    public Optional<Sale> findById(Long transactionId) {
        return transactionRepository.findById(transactionId).map(this::toDomain);
    }

    @Override
    public Sale save(Sale sale) {
        SalesTransactionEntity transaction = transactionRepository.save(toEntity(sale));
        Long transactionId = transaction.getTransactionId();

        List<SaleItemEntity> items = sale.getItems().stream()
                .map(item -> itemRepository.save(toEntity(item, transactionId)))
                .toList();

        List<PaymentEntity> payments = sale.getPayments().stream()
                .map(payment -> paymentRepository.save(toEntity(payment, transactionId)))
                .toList();

        return toDomain(transaction, items, payments);
    }

    private Sale toDomain(SalesTransactionEntity transaction) {
        List<SaleItemEntity> items = itemRepository.findByTransactionId(transaction.getTransactionId());
        List<PaymentEntity> payments = paymentRepository.findByTransactionId(transaction.getTransactionId());
        return toDomain(transaction, items, payments);
    }

    private Sale toDomain(SalesTransactionEntity transaction, List<SaleItemEntity> items, List<PaymentEntity> payments) {
        return Sale.builder()
                .transactionId(transaction.getTransactionId())
                .transactionDate(transaction.getTransactionDate())
                .employeeId(transaction.getEmployeeId())
                .locationId(transaction.getLocationId())
                .customerId(transaction.getCustomerId())
                .totalAmount(transaction.getTotalAmount())
                .discountAmount(transaction.getDiscountAmount())
                .taxAmount(transaction.getTaxAmount())
                .paymentStatus(transaction.getPaymentStatus())
                .clientEventId(transaction.getClientEventId())
                .cancelledAt(transaction.getCancelledAt())
                .invoicePrefix(transaction.getInvoicePrefix())
                .invoiceNumber(transaction.getInvoiceNumber())
                .items(items.stream().map(this::toDomain).toList())
                .payments(payments.stream().map(this::toDomain).toList())
                .build();
    }

    private SaleItem toDomain(SaleItemEntity entity) {
        return SaleItem.builder()
                .saleItemId(entity.getSaleItemId())
                .variantId(entity.getVariantId())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .discountPerItem(entity.getDiscountPerItem())
                .subtotal(entity.getSubtotal())
                .productUnitId(entity.getProductUnitId())
                .unitName(entity.getUnitName())
                .conversionFactor(entity.getConversionFactor())
                .build();
    }

    private Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
                .paymentId(entity.getPaymentId())
                .paymentMethodId(entity.getPaymentMethodId())
                .amount(entity.getAmount())
                .paymentDate(entity.getPaymentDate())
                .referenceNumber(entity.getReferenceNumber())
                .build();
    }

    private SalesTransactionEntity toEntity(Sale sale) {
        SalesTransactionEntity entity = new SalesTransactionEntity();
        entity.setTransactionId(sale.getTransactionId());
        entity.setEmployeeId(sale.getEmployeeId());
        entity.setCustomerId(sale.getCustomerId());
        entity.setTotalAmount(sale.getTotalAmount());
        entity.setDiscountAmount(sale.getDiscountAmount());
        entity.setTaxAmount(sale.getTaxAmount());
        entity.setPaymentStatus(sale.getPaymentStatus());
        entity.setLocationId(sale.getLocationId());
        entity.setClientEventId(sale.getClientEventId());
        entity.setCancelledAt(sale.getCancelledAt());
        entity.setInvoicePrefix(sale.getInvoicePrefix());
        entity.setInvoiceNumber(sale.getInvoiceNumber());
        return entity;
    }

    private SaleItemEntity toEntity(SaleItem item, Long transactionId) {
        SaleItemEntity entity = new SaleItemEntity();
        entity.setSaleItemId(item.getSaleItemId());
        entity.setTransactionId(transactionId);
        entity.setVariantId(item.getVariantId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPrice(item.getUnitPrice());
        entity.setDiscountPerItem(item.getDiscountPerItem());
        entity.setSubtotal(item.getSubtotal());
        entity.setProductUnitId(item.getProductUnitId());
        entity.setUnitName(item.getUnitName());
        entity.setConversionFactor(item.getConversionFactor());
        return entity;
    }

    private PaymentEntity toEntity(Payment payment, Long transactionId) {
        PaymentEntity entity = new PaymentEntity();
        entity.setPaymentId(payment.getPaymentId());
        entity.setTransactionId(transactionId);
        entity.setPaymentMethodId(payment.getPaymentMethodId());
        entity.setAmount(payment.getAmount());
        entity.setReferenceNumber(payment.getReferenceNumber());
        return entity;
    }
}
