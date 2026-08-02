package co.com.galfields.pos_transactions.api.report;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.report.InvoiceDetail;
import co.com.galfields.pos_transactions.model.report.InvoiceSummary;
import co.com.galfields.pos_transactions.model.report.PaymentMethodSales;
import co.com.galfields.pos_transactions.model.report.SalesSummary;
import co.com.galfields.pos_transactions.usecase.report.ReportUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerTest {

    @Mock
    private ReportUseCase reportUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void salesSummaryReturnsAggregate() throws Exception {
        when(reportUseCase.salesSummary(any(), any()))
                .thenReturn(new SalesSummary(BigDecimal.valueOf(9000), 2, BigDecimal.valueOf(4500)));

        mockMvc.perform(get("/api/reports/sales-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSales").value(9000))
                .andExpect(jsonPath("$.transactionCount").value(2));
    }

    @Test
    void salesByPaymentMethodReturnsList() throws Exception {
        when(reportUseCase.salesByPaymentMethod(any(), any()))
                .thenReturn(List.of(new PaymentMethodSales(1L, "Efectivo", BigDecimal.valueOf(9000), 2)));

        mockMvc.perform(get("/api/reports/sales-by-payment-method"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].methodName").value("Efectivo"));
    }

    @Test
    void invoicesReturnsPagedContent() throws Exception {
        InvoiceSummary summary = new InvoiceSummary(1L, LocalDateTime.now(), "John Doe", BigDecimal.TEN,
                BigDecimal.ZERO, 1, null, "F", "001");
        when(reportUseCase.invoices(any(), any(), any())).thenReturn(new PageResult<>(List.of(summary), 1, 1, 0, 20));

        mockMvc.perform(get("/api/reports/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void invoiceDetailReturnsFullBreakdown() throws Exception {
        InvoiceDetail detail = new InvoiceDetail(1L, LocalDateTime.now(), "John Doe", BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), null, "F", "001");
        when(reportUseCase.invoiceDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/reports/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(1))
                .andExpect(jsonPath("$.employeeName").value("John Doe"));
    }

    @Test
    void invoiceDetailReturns404WhenNotFound() throws Exception {
        when(reportUseCase.invoiceDetail(99L)).thenThrow(new ResourceNotFoundException("Transaction 99 not found"));

        mockMvc.perform(get("/api/reports/invoices/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inventoryReturnsPagedContent() throws Exception {
        when(reportUseCase.inventory(any())).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void lowStockPassesThresholdThrough() throws Exception {
        when(reportUseCase.lowStock(eq(3), any())).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/reports/low-stock?threshold=3"))
                .andExpect(status().isOk());
    }
}
