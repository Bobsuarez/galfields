package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.SaleLineRequest;
import co.com.galfields.pos.dto.SalePaymentRequest;
import co.com.galfields.pos.dto.SaleRequest;
import co.com.galfields.pos.dto.SaleResponse;
import co.com.galfields.pos.dto.StockAdjustmentBatchRequest;
import co.com.galfields.pos.dto.StockAdjustmentItemRequest;
import co.com.galfields.pos.entity.Employee;
import co.com.galfields.pos.entity.Location;
import co.com.galfields.pos.entity.Payment;
import co.com.galfields.pos.entity.PaymentMethod;
import co.com.galfields.pos.entity.PaymentStatus;
import co.com.galfields.pos.entity.ProductVariant;
import co.com.galfields.pos.entity.SaleItem;
import co.com.galfields.pos.entity.SalesTransaction;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.EmployeeRepository;
import co.com.galfields.pos.repository.LocationRepository;
import co.com.galfields.pos.repository.PaymentMethodRepository;
import co.com.galfields.pos.repository.PaymentRepository;
import co.com.galfields.pos.repository.ProductVariantRepository;
import co.com.galfields.pos.repository.SaleItemRepository;
import co.com.galfields.pos.repository.SalesTransactionRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a completed sale reported by a POS terminal (currently only
 * {@code apps/galfield-pos}) — one call per sale, replacing what that
 * terminal used to send to {@code POST /api/inventory/adjustments} alone.
 * Writes {@code sales_transactions}/{@code sale_items}/{@code payments} AND
 * applies the matching stock adjustment in the same transaction, by reusing
 * {@link InventoryService#applyAdjustments} under the same
 * {@code clientEventId} — so the sale record and the stock decrement are
 * atomic and share one idempotency key end to end.
 *
 * Idempotent by {@code clientEventId}: a retried report (the terminal
 * applied it locally but never saw the response) returns the
 * already-created transaction instead of duplicating it.
 *
 * Employee attribution is a placeholder: the desktop POS has no real
 * per-cashier login yet, so every sale is attributed to a single seeded
 * "pos-terminal" employee (see {@code V4__sales_recording.sql}) until that
 * exists.
 */
@Service
@RequiredArgsConstructor
public class SalesService {

    private static final String DEFAULT_LOCATION_NAME = "Bogotá - Chapinero";
    private static final String DEFAULT_EMPLOYEE_USERNAME = "pos-terminal";

    private final SalesTransactionRepository salesTransactionRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final EmployeeRepository employeeRepository;
    private final LocationRepository locationRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final InventoryService inventoryService;

    @Transactional
    public SaleResponse recordSale(SaleRequest request) {
        var existing = salesTransactionRepository.findByClientEventId(request.clientEventId());
        if (existing.isPresent()) {
            return new SaleResponse(existing.get().getTransactionId(), request.clientEventId(), true);
        }

        Location location = defaultLocation();

        SalesTransaction transaction = new SalesTransaction();
        transaction.setClientEventId(request.clientEventId());
        transaction.setEmployee(defaultEmployee());
        transaction.setLocation(location);
        transaction.setTotalAmount(request.totalAmount());
        transaction.setDiscountAmount(request.discountAmount());
        transaction.setTaxAmount(BigDecimal.ZERO);
        transaction.setPaymentStatus(PaymentStatus.Paid);
        salesTransactionRepository.save(transaction);

        for (SaleLineRequest line : request.items()) {
            ProductVariant variant = productVariantRepository.findById(line.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant " + line.variantId() + " not found"));

            SaleItem saleItem = new SaleItem();
            saleItem.setTransaction(transaction);
            saleItem.setVariant(variant);
            saleItem.setQuantity(line.quantity());
            saleItem.setUnitPrice(line.unitPrice());
            saleItem.setSubtotal(line.subtotal());
            saleItemRepository.save(saleItem);
        }

        for (SalePaymentRequest p : request.payments()) {
            PaymentMethod method = paymentMethodRepository.findById(p.paymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment method " + p.paymentMethodId() + " not found"));

            Payment payment = new Payment();
            payment.setTransaction(transaction);
            payment.setPaymentMethod(method);
            payment.setAmount(p.amount());
            payment.setReferenceNumber(p.referenceNumber());
            paymentRepository.save(payment);
        }

        // Same clientEventId as the sale itself - reuses the exact idempotent
        // stock-decrement logic /api/inventory/adjustments already has,
        // instead of duplicating it here.
        var stockItems = request.items().stream()
                .map(line -> new StockAdjustmentItemRequest(line.variantId(), -line.quantity()))
                .toList();
        inventoryService.applyAdjustments(new StockAdjustmentBatchRequest(request.clientEventId(), stockItems));

        return new SaleResponse(transaction.getTransactionId(), request.clientEventId(), false);
    }

    private Location defaultLocation() {
        return locationRepository.findByName(DEFAULT_LOCATION_NAME)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Default location '" + DEFAULT_LOCATION_NAME + "' not found"));
    }

    private Employee defaultEmployee() {
        return employeeRepository.findByUsername(DEFAULT_EMPLOYEE_USERNAME)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Default employee '" + DEFAULT_EMPLOYEE_USERNAME + "' not found"));
    }
}
