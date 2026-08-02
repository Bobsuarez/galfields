package co.com.galfields.pos_transactions.api.sale;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.InvalidStateException;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.sale.PaymentStatus;
import co.com.galfields.pos_transactions.model.sale.Sale;
import co.com.galfields.pos_transactions.usecase.sale.CancelSaleUseCase;
import co.com.galfields.pos_transactions.usecase.sale.RecordSaleResult;
import co.com.galfields.pos_transactions.usecase.sale.RecordSaleUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalesControllerTest {

    @Mock
    private RecordSaleUseCase recordSaleUseCase;
    @Mock
    private CancelSaleUseCase cancelSaleUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SalesController controller = new SalesController(recordSaleUseCase, cancelSaleUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void recordSaleReturns200WithTransactionId() throws Exception {
        Sale saved = Sale.builder().transactionId(99L).clientEventId("evt-1").paymentStatus(PaymentStatus.Paid).build();
        when(recordSaleUseCase.execute(any())).thenReturn(new RecordSaleResult(saved, false));

        String body = """
                {
                  "clientEventId": "evt-1",
                  "items": [{ "variantId": 12, "quantity": 2, "unitPrice": 4500.00, "subtotal": 9000.00 }],
                  "payments": [{ "paymentMethodId": 3, "amount": 9000.00 }],
                  "discountAmount": 0,
                  "totalAmount": 9000.00
                }
                """;

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "transactionId": 99, "clientEventId": "evt-1", "alreadyProcessed": false }
                        """));
    }

    @Test
    void recordSaleWithoutItemsReturns400() throws Exception {
        String body = """
                {
                  "clientEventId": "evt-1",
                  "items": [],
                  "payments": [{ "paymentMethodId": 3, "amount": 9000.00 }],
                  "discountAmount": 0,
                  "totalAmount": 9000.00
                }
                """;

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordSaleWithUnknownVariantReturns404() throws Exception {
        when(recordSaleUseCase.execute(any())).thenThrow(new ResourceNotFoundException("Variant 999 not found"));

        String body = """
                {
                  "clientEventId": "evt-1",
                  "items": [{ "variantId": 999, "quantity": 1, "unitPrice": 1.00, "subtotal": 1.00 }],
                  "payments": [{ "paymentMethodId": 3, "amount": 1.00 }],
                  "discountAmount": 0,
                  "totalAmount": 1.00
                }
                """;

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Variant 999 not found")));
    }

    @Test
    void cancelReturns204() throws Exception {
        mockMvc.perform(post("/api/sales/1/cancel"))
                .andExpect(status().isNoContent());
        verify(cancelSaleUseCase).cancelByTransactionId(1L);
    }

    @Test
    void cancelByClientEventReturns204() throws Exception {
        mockMvc.perform(post("/api/sales/by-client-event/evt-1/cancel"))
                .andExpect(status().isNoContent());
        verify(cancelSaleUseCase).cancelByClientEventId("evt-1");
    }

    @Test
    void cancelAlreadyCancelledReturns409() throws Exception {
        doThrow(new InvalidStateException("Esta factura ya está cancelada"))
                .when(cancelSaleUseCase).cancelByTransactionId(anyLong());

        mockMvc.perform(post("/api/sales/1/cancel"))
                .andExpect(status().isConflict());
    }
}
