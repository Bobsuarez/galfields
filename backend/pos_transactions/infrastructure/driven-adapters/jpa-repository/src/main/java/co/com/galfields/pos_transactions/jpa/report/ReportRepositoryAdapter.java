package co.com.galfields.pos_transactions.jpa.report;

import co.com.galfields.pos_transactions.model.PageQuery;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.report.InventoryRow;
import co.com.galfields.pos_transactions.model.report.InvoiceDetail;
import co.com.galfields.pos_transactions.model.report.InvoiceLine;
import co.com.galfields.pos_transactions.model.report.InvoicePayment;
import co.com.galfields.pos_transactions.model.report.InvoiceSummary;
import co.com.galfields.pos_transactions.model.report.PaymentMethodSales;
import co.com.galfields.pos_transactions.model.report.SalesSummary;
import co.com.galfields.pos_transactions.model.report.gateways.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryAdapter implements ReportRepository {

    private final ReportSalesJpaRepository salesRepository;
    private final ReportPaymentJpaRepository paymentRepository;
    private final ReportSaleItemJpaRepository saleItemRepository;
    private final ReportInventoryJpaRepository inventoryRepository;

    @Override
    public SalesSummary salesSummary(LocalDateTime from, LocalDateTime to) {
        SalesSummaryProjection projection = salesRepository.summarize(from, to);
        return new SalesSummary(projection.getTotalSales(), projection.getTransactionCount(), projection.getAverageTicket());
    }

    @Override
    public List<PaymentMethodSales> salesByPaymentMethod(LocalDateTime from, LocalDateTime to) {
        return paymentRepository.summarizeByPaymentMethod(from, to).stream()
                .map(p -> new PaymentMethodSales(p.getPaymentMethodId(), p.getMethodName(), p.getTotalAmount(), p.getTransactionCount()))
                .toList();
    }

    @Override
    public PageResult<InvoiceSummary> invoices(LocalDateTime from, LocalDateTime to, PageQuery query) {
        Page<InvoiceSummaryProjection> page = salesRepository.findInvoiceSummaries(from, to, toPageable(query));
        return new PageResult<>(
                page.getContent().stream()
                        .map(p -> new InvoiceSummary(p.getTransactionId(), p.getTransactionDate(), p.getEmployeeName(),
                                p.getTotalAmount(), p.getDiscountAmount(), p.getItemCount(), p.getCancelledAt(),
                                p.getInvoicePrefix(), p.getInvoiceNumber()))
                        .toList(),
                page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    @Override
    public Optional<InvoiceDetail> invoiceDetail(Long transactionId) {
        return salesRepository.findInvoiceHeaderById(transactionId).map(header -> {
            List<InvoiceLine> items = saleItemRepository.findLinesByTransactionId(transactionId).stream()
                    .map(l -> new InvoiceLine(l.getProductName(), l.getSku(), l.getQuantity(), l.getUnitPrice(),
                            l.getSubtotal(), l.getUnitName(), l.getConversionFactor()))
                    .toList();
            List<InvoicePayment> payments = paymentRepository.findPaymentsByTransactionId(transactionId).stream()
                    .map(p -> new InvoicePayment(p.getMethodName(), p.getAmount(), p.getReferenceNumber()))
                    .toList();
            return new InvoiceDetail(header.getTransactionId(), header.getTransactionDate(), header.getEmployeeName(),
                    header.getTotalAmount(), header.getDiscountAmount(), header.getTaxAmount(), items, payments,
                    header.getCancelledAt(), header.getInvoicePrefix(), header.getInvoiceNumber());
        });
    }

    @Override
    public PageResult<InventoryRow> inventory(PageQuery query) {
        return toPageResult(inventoryRepository.findAllRows(toPageable(query)));
    }

    @Override
    public PageResult<InventoryRow> lowStock(int threshold, PageQuery query) {
        return toPageResult(inventoryRepository.findLowStockRows(threshold, toPageable(query)));
    }

    private PageResult<InventoryRow> toPageResult(Page<InventoryRowProjection> page) {
        return new PageResult<>(
                page.getContent().stream()
                        .map(r -> new InventoryRow(r.getVariantId(), r.getSku(), r.getProductName(), r.getCategoryName(),
                                r.getLocationName(), r.getQuantityOnHand()))
                        .toList(),
                page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    // Sorting isn't offered for reports (matches backend/pos's ReportController,
    // which has no sortable-property whitelist here unlike ProductController) —
    // only page/size are honored, the SQL above already has a sensible fixed order.
    private PageRequest toPageable(PageQuery query) {
        return PageRequest.of(query.page(), query.size());
    }
}
