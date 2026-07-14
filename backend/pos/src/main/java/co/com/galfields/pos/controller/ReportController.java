package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.InvoiceDetailResponse;
import co.com.galfields.pos.dto.InvoiceSummaryResponse;
import co.com.galfields.pos.dto.InventoryRowResponse;
import co.com.galfields.pos.dto.PaymentMethodSalesResponse;
import co.com.galfields.pos.dto.SalesSummaryResponse;
import co.com.galfields.pos.service.ReportService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only report endpoints backing the mobile app's report screens (see
 * apps/galfields-mobile's CLAUDE.md). {@code from}/{@code to} are plain
 * dates (inclusive) — omitting both defaults to "today"; omitting just
 * {@code from} scopes to the single {@code to} day. See ReportService for
 * which report covers which table, and for the "cierre de caja = same
 * aggregate as ventas por método de pago, scoped to one day" design note.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales-summary")
    public SalesSummaryResponse salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesSummary(startOf(from, to), endOf(to));
    }

    @GetMapping("/sales-by-payment-method")
    public List<PaymentMethodSalesResponse> salesByPaymentMethod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesByPaymentMethod(startOf(from, to), endOf(to));
    }

    @GetMapping("/invoices")
    public PagedModel<InvoiceSummaryResponse> invoices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InvoiceSummaryResponse> page = reportService.invoices(startOf(from, to), endOf(to), pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/invoices/{transactionId}")
    public InvoiceDetailResponse invoiceDetail(@PathVariable Long transactionId) {
        return reportService.invoiceDetail(transactionId);
    }

    @GetMapping("/inventory")
    public PagedModel<InventoryRowResponse> inventory(@PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(reportService.inventory(pageable));
    }

    @GetMapping("/low-stock")
    public PagedModel<InventoryRowResponse> lowStock(
            @RequestParam(required = false) Integer threshold,
            @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(reportService.lowStock(threshold, pageable));
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
}
