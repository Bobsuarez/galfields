package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.InvoiceDetailResponse;
import co.com.galfields.pos.dto.InvoiceLineResponse;
import co.com.galfields.pos.dto.InvoicePaymentResponse;
import co.com.galfields.pos.dto.InvoiceSummaryResponse;
import co.com.galfields.pos.dto.InventoryRowResponse;
import co.com.galfields.pos.dto.PaymentMethodSalesResponse;
import co.com.galfields.pos.dto.SalesSummaryResponse;
import co.com.galfields.pos.entity.SalesTransaction;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.InventoryRepository;
import co.com.galfields.pos.repository.PaymentRepository;
import co.com.galfields.pos.repository.SaleItemRepository;
import co.com.galfields.pos.repository.SalesTransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only aggregation queries over sales/inventory for the mobile app's
 * report screens (see apps/galfields-mobile's CLAUDE.md). Every date-ranged
 * report takes an inclusive {@code [from, to]} range at day granularity —
 * callers pass {@code from} at 00:00:00 and {@code to} at 23:59:59 of the
 * last day wanted (see ReportController, which builds these from a plain
 * {@code LocalDate}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final SalesTransactionRepository salesTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final SaleItemRepository saleItemRepository;
    private final InventoryRepository inventoryRepository;

    /** Covers "Ventas del día" (mobile calls with from=to=today). */
    public SalesSummaryResponse salesSummary(LocalDateTime from, LocalDateTime to) {
        return salesTransactionRepository.summarize(from, to);
    }

    /**
     * Covers both "Ventas por método de pago" and "Cierre de caja" — the
     * latter is this same aggregate scoped to a single day, by design (see
     * backend/pos's CLAUDE.md): no separate cash-session endpoint exists.
     */
    public List<PaymentMethodSalesResponse> salesByPaymentMethod(LocalDateTime from, LocalDateTime to) {
        return paymentRepository.summarizeByPaymentMethod(from, to);
    }

    /** Covers "Historial de facturas". */
    public Page<InvoiceSummaryResponse> invoices(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return salesTransactionRepository.findInvoiceSummaries(from, to, pageable);
    }

    /** Detail view for one invoice from the history list above. */
    public InvoiceDetailResponse invoiceDetail(Long transactionId) {
        SalesTransaction transaction = salesTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction " + transactionId + " not found"));

        List<InvoiceLineResponse> items = saleItemRepository.findByTransaction_TransactionId(transactionId).stream()
                .map(item -> new InvoiceLineResponse(
                        item.getVariant().getProduct().getName(),
                        item.getVariant().getSku(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();

        List<InvoicePaymentResponse> payments = paymentRepository.findByTransaction_TransactionId(transactionId).stream()
                .map(p -> new InvoicePaymentResponse(
                        p.getPaymentMethod().getMethodName(),
                        p.getAmount(),
                        p.getReferenceNumber()))
                .toList();

        return new InvoiceDetailResponse(
                transaction.getTransactionId(),
                transaction.getTransactionDate(),
                transaction.getEmployee().getFirstName() + " " + transaction.getEmployee().getLastName(),
                transaction.getTotalAmount(),
                transaction.getDiscountAmount(),
                transaction.getTaxAmount(),
                items,
                payments);
    }

    /** Covers "Inventario actual". */
    public Page<InventoryRowResponse> inventory(Pageable pageable) {
        return inventoryRepository.findAllRows(pageable);
    }

    /**
     * Covers "Productos con stock bajo". {@code products}/{@code
     * product_variants} have no per-product minimum-stock column yet (same
     * gap noted in apps/galfield-pos's CLAUDE.md), so the threshold is a
     * query param defaulting to {@value #DEFAULT_LOW_STOCK_THRESHOLD} —
     * matching the desktop POS's own hardcoded LOW_STOCK_THRESHOLD.
     */
    public Page<InventoryRowResponse> lowStock(Integer threshold, Pageable pageable) {
        int effectiveThreshold = threshold != null ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
        return inventoryRepository.findLowStockRows(effectiveThreshold, pageable);
    }
}
