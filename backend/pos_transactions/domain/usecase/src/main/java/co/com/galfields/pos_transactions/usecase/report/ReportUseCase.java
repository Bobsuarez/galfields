package co.com.galfields.pos_transactions.usecase.report;

import co.com.galfields.pos_transactions.model.PageQuery;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.report.InventoryRow;
import co.com.galfields.pos_transactions.model.report.InvoiceDetail;
import co.com.galfields.pos_transactions.model.report.InvoiceSummary;
import co.com.galfields.pos_transactions.model.report.PaymentMethodSales;
import co.com.galfields.pos_transactions.model.report.SalesSummary;
import co.com.galfields.pos_transactions.model.report.gateways.ReportRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mirrors backend/pos's ReportService. Purely read-only aggregation over
 * sales/inventory; from/to range resolution ("today" default) stays in the
 * controller, same split backend/pos uses.
 */
@RequiredArgsConstructor
public class ReportUseCase {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final ReportRepository reportRepository;

    public SalesSummary salesSummary(LocalDateTime from, LocalDateTime to) {
        return reportRepository.salesSummary(from, to);
    }

    public List<PaymentMethodSales> salesByPaymentMethod(LocalDateTime from, LocalDateTime to) {
        return reportRepository.salesByPaymentMethod(from, to);
    }

    public PageResult<InvoiceSummary> invoices(LocalDateTime from, LocalDateTime to, PageQuery query) {
        return reportRepository.invoices(from, to, query);
    }

    public InvoiceDetail invoiceDetail(Long transactionId) {
        return reportRepository.invoiceDetail(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction " + transactionId + " not found"));
    }

    public PageResult<InventoryRow> inventory(PageQuery query) {
        return reportRepository.inventory(query);
    }

    public PageResult<InventoryRow> lowStock(Integer threshold, PageQuery query) {
        int effectiveThreshold = threshold != null ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
        return reportRepository.lowStock(effectiveThreshold, query);
    }
}
