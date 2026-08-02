package co.com.galfields.pos_transactions.api.report;

import co.com.galfields.pos_transactions.model.PageQuery;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.report.InventoryRow;
import co.com.galfields.pos_transactions.model.report.InvoiceDetail;
import co.com.galfields.pos_transactions.model.report.InvoiceLine;
import co.com.galfields.pos_transactions.model.report.InvoicePayment;
import co.com.galfields.pos_transactions.model.report.InvoiceSummary;
import co.com.galfields.pos_transactions.model.report.PaymentMethodSales;
import co.com.galfields.pos_transactions.model.report.SalesSummary;
import co.com.galfields.pos_transactions.usecase.report.ReportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Mirrors backend/pos's ReportController 1:1 — same from/to (inclusive day
 * range, defaulting to today) resolution, same 5 endpoints.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportUseCase reportUseCase;

    @GetMapping("/sales-summary")
    @Transactional(readOnly = true)
    public SalesSummaryResponse salesSummary(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return toResponse(reportUseCase.salesSummary(startOf(from, to), endOf(to)));
    }

    @GetMapping("/sales-by-payment-method")
    @Transactional(readOnly = true)
    public List<PaymentMethodSalesResponse> salesByPaymentMethod(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportUseCase.salesByPaymentMethod(startOf(from, to), endOf(to)).stream().map(this::toResponse).toList();
    }

    @GetMapping("/invoices")
    @Transactional(readOnly = true)
    public PagedResponse<InvoiceSummaryResponse> invoices(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResult<InvoiceSummary> page = reportUseCase.invoices(startOf(from, to), endOf(to), toPageQuery(pageable));
        return new PagedResponse<>(page.content().stream().map(this::toResponse).toList(),
                page.totalElements(), page.totalPages(), page.page(), page.size());
    }

    @GetMapping("/invoices/{transactionId}")
    @Transactional(readOnly = true)
    public InvoiceDetailResponse invoiceDetail(@PathVariable("transactionId") Long transactionId) {
        return toResponse(reportUseCase.invoiceDetail(transactionId));
    }

    @GetMapping("/inventory")
    @Transactional(readOnly = true)
    public PagedResponse<InventoryRowResponse> inventory(@PageableDefault(size = 20) Pageable pageable) {
        PageResult<InventoryRow> page = reportUseCase.inventory(toPageQuery(pageable));
        return new PagedResponse<>(page.content().stream().map(this::toResponse).toList(),
                page.totalElements(), page.totalPages(), page.page(), page.size());
    }

    @GetMapping("/low-stock")
    @Transactional(readOnly = true)
    public PagedResponse<InventoryRowResponse> lowStock(
            @RequestParam(name = "threshold", required = false) Integer threshold,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResult<InventoryRow> page = reportUseCase.lowStock(threshold, toPageQuery(pageable));
        return new PagedResponse<>(page.content().stream().map(this::toResponse).toList(),
                page.totalElements(), page.totalPages(), page.page(), page.size());
    }

    private LocalDateTime startOf(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo;
        return effectiveFrom.atStartOfDay();
    }

    private LocalDateTime endOf(LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        return effectiveTo.atTime(LocalTime.MAX);
    }

    // No sortable-property whitelist here (matches backend/pos's
    // ReportController — reports don't expose one) — only page/size carry
    // through, see ReportRepositoryAdapter#toPageable.
    private PageQuery toPageQuery(Pageable pageable) {
        return new PageQuery(pageable.getPageNumber(), pageable.getPageSize(), List.of());
    }

    private SalesSummaryResponse toResponse(SalesSummary summary) {
        return new SalesSummaryResponse(summary.totalSales(), summary.transactionCount(), summary.averageTicket());
    }

    private PaymentMethodSalesResponse toResponse(PaymentMethodSales sales) {
        return new PaymentMethodSalesResponse(sales.paymentMethodId(), sales.methodName(), sales.totalAmount(), sales.transactionCount());
    }

    private InvoiceSummaryResponse toResponse(InvoiceSummary summary) {
        return new InvoiceSummaryResponse(summary.transactionId(), summary.transactionDate(), summary.employeeName(),
                summary.totalAmount(), summary.discountAmount(), summary.itemCount(), summary.cancelledAt(),
                summary.invoicePrefix(), summary.invoiceNumber());
    }

    private InvoiceDetailResponse toResponse(InvoiceDetail detail) {
        List<InvoiceLineResponse> items = detail.items().stream().map(this::toResponse).toList();
        List<InvoicePaymentResponse> payments = detail.payments().stream().map(this::toResponse).toList();
        return new InvoiceDetailResponse(detail.transactionId(), detail.transactionDate(), detail.employeeName(),
                detail.totalAmount(), detail.discountAmount(), detail.taxAmount(), items, payments,
                detail.cancelledAt(), detail.invoicePrefix(), detail.invoiceNumber());
    }

    private InvoiceLineResponse toResponse(InvoiceLine line) {
        return new InvoiceLineResponse(line.productName(), line.sku(), line.quantity(), line.unitPrice(),
                line.subtotal(), line.unitName(), line.conversionFactor());
    }

    private InvoicePaymentResponse toResponse(InvoicePayment payment) {
        return new InvoicePaymentResponse(payment.methodName(), payment.amount(), payment.referenceNumber());
    }

    private InventoryRowResponse toResponse(InventoryRow row) {
        return new InventoryRowResponse(row.variantId(), row.sku(), row.productName(), row.categoryName(),
                row.locationName(), row.quantityOnHand());
    }

    public record PagedResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    }
}
