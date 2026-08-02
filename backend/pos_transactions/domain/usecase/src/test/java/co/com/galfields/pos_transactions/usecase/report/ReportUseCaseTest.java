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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportUseCaseTest {

    @Mock
    private ReportRepository reportRepository;

    private ReportUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ReportUseCase(reportRepository);
    }

    @Test
    void salesSummaryDelegatesToRepository() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 1, 23, 59);
        SalesSummary summary = new SalesSummary(BigDecimal.TEN, 2, BigDecimal.valueOf(5));
        when(reportRepository.salesSummary(from, to)).thenReturn(summary);

        assertThat(useCase.salesSummary(from, to)).isEqualTo(summary);
    }

    @Test
    void salesByPaymentMethodDelegatesToRepository() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now();
        List<PaymentMethodSales> sales = List.of(new PaymentMethodSales(1L, "Efectivo", BigDecimal.TEN, 1));
        when(reportRepository.salesByPaymentMethod(from, to)).thenReturn(sales);

        assertThat(useCase.salesByPaymentMethod(from, to)).isEqualTo(sales);
    }

    @Test
    void invoicesDelegatesToRepository() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now();
        PageQuery query = new PageQuery(0, 20, List.of());
        PageResult<InvoiceSummary> page = new PageResult<>(List.of(), 0, 0, 0, 20);
        when(reportRepository.invoices(from, to, query)).thenReturn(page);

        assertThat(useCase.invoices(from, to, query)).isEqualTo(page);
    }

    @Test
    void invoiceDetailReturnsWhenFound() {
        InvoiceDetail detail = new InvoiceDetail(1L, LocalDateTime.now(), "John Doe", BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), null, null, null);
        when(reportRepository.invoiceDetail(1L)).thenReturn(Optional.of(detail));

        assertThat(useCase.invoiceDetail(1L)).isEqualTo(detail);
    }

    @Test
    void invoiceDetailThrowsWhenNotFound() {
        when(reportRepository.invoiceDetail(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.invoiceDetail(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inventoryDelegatesToRepository() {
        PageQuery query = new PageQuery(0, 20, List.of());
        PageResult<InventoryRow> page = new PageResult<>(List.of(), 0, 0, 0, 20);
        when(reportRepository.inventory(query)).thenReturn(page);

        assertThat(useCase.inventory(query)).isEqualTo(page);
    }

    @Test
    void lowStockUsesDefaultThresholdWhenNull() {
        PageQuery query = new PageQuery(0, 20, List.of());
        when(reportRepository.lowStock(eq(5), any())).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 20));

        useCase.lowStock(null, query);

        ArgumentCaptor<Integer> thresholdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(reportRepository).lowStock(thresholdCaptor.capture(), eq(query));
        assertThat(thresholdCaptor.getValue()).isEqualTo(5);
    }

    @Test
    void lowStockUsesGivenThreshold() {
        PageQuery query = new PageQuery(0, 20, List.of());
        when(reportRepository.lowStock(eq(10), any())).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 20));

        useCase.lowStock(10, query);

        verify(reportRepository).lowStock(10, query);
    }
}
