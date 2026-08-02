package co.com.galfields.pos_transactions.model.report.gateways;

import co.com.galfields.pos_transactions.model.PageQuery;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.report.InventoryRow;
import co.com.galfields.pos_transactions.model.report.InvoiceDetail;
import co.com.galfields.pos_transactions.model.report.InvoiceSummary;
import co.com.galfields.pos_transactions.model.report.PaymentMethodSales;
import co.com.galfields.pos_transactions.model.report.SalesSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {

    SalesSummary salesSummary(LocalDateTime from, LocalDateTime to);

    List<PaymentMethodSales> salesByPaymentMethod(LocalDateTime from, LocalDateTime to);

    PageResult<InvoiceSummary> invoices(LocalDateTime from, LocalDateTime to, PageQuery query);

    Optional<InvoiceDetail> invoiceDetail(Long transactionId);

    PageResult<InventoryRow> inventory(PageQuery query);

    PageResult<InventoryRow> lowStock(int threshold, PageQuery query);
}
