package co.com.galfields.pos_transactions.api.invoicing;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.invoicing.InvoiceNumberingRange;
import co.com.galfields.pos_transactions.usecase.invoicing.InvoiceNumberingRangeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvoiceNumberingRangeControllerTest {

    @Mock
    private InvoiceNumberingRangeUseCase invoiceNumberingRangeUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new InvoiceNumberingRangeController(invoiceNumberingRangeUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201WithDenormalizedTerminalCode() throws Exception {
        InvoiceNumberingRange saved = InvoiceNumberingRange.builder().rangeId(1L).terminalId(10L).terminalCode("T1")
                .prefix("FE").rangeStart(1L).rangeEnd(1000L).build();
        when(invoiceNumberingRangeUseCase.create(any())).thenReturn(saved);

        mockMvc.perform(post("/api/invoice-numbering-ranges").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":10,\"prefix\":\"FE\",\"rangeStart\":1,\"rangeEnd\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rangeId").value(1))
                .andExpect(jsonPath("$.terminalCode").value("T1"));
    }

    @Test
    void getByTerminalReturns404WhenNoneAssigned() throws Exception {
        when(invoiceNumberingRangeUseCase.getByTerminalCode(anyString()))
                .thenThrow(new ResourceNotFoundException("No hay rango de facturación asignado para la terminal 'T9'"));

        mockMvc.perform(get("/api/invoice-numbering-ranges/by-terminal/T9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithoutTerminalIdReturns400() throws Exception {
        mockMvc.perform(post("/api/invoice-numbering-ranges").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prefix\":\"FE\",\"rangeStart\":1,\"rangeEnd\":1000}"))
                .andExpect(status().isBadRequest());
    }
}
