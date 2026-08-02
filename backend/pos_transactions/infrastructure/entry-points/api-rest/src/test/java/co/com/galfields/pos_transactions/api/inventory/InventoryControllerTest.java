package co.com.galfields.pos_transactions.api.inventory;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.inventory.StockAdjustmentResult;
import co.com.galfields.pos_transactions.usecase.inventory.ApplyStockAdjustmentsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryControllerTest {

    @Mock
    private ApplyStockAdjustmentsUseCase applyStockAdjustmentsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        InventoryController controller = new InventoryController(applyStockAdjustmentsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void applyAdjustmentsReturnsResultsPerItem() throws Exception {
        when(applyStockAdjustmentsUseCase.execute(anyString(), anyList())).thenReturn(List.of(
                new StockAdjustmentResult(12L, false, -2),
                new StockAdjustmentResult(45L, false, -1)));

        String body = """
                {
                  "clientEventId": "evt-1",
                  "items": [
                    { "variantId": 12, "quantityDelta": -2 },
                    { "variantId": 45, "quantityDelta": -1 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/inventory/adjustments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "clientEventId": "evt-1",
                          "results": [
                            { "variantId": 12, "alreadyProcessed": false, "resultingQuantity": -2 },
                            { "variantId": 45, "alreadyProcessed": false, "resultingQuantity": -1 }
                          ]
                        }
                        """));
    }

    @Test
    void emptyItemsReturns400() throws Exception {
        String body = """
                { "clientEventId": "evt-1", "items": [] }
                """;

        mockMvc.perform(post("/api/inventory/adjustments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownVariantReturns404() throws Exception {
        when(applyStockAdjustmentsUseCase.execute(anyString(), anyList()))
                .thenThrow(new ResourceNotFoundException("Variant 999 not found"));

        String body = """
                { "clientEventId": "evt-1", "items": [{ "variantId": 999, "quantityDelta": -1 }] }
                """;

        mockMvc.perform(post("/api/inventory/adjustments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
}
