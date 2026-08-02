package co.com.galfields.pos_transactions.api.catalog;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.catalog.Product;
import co.com.galfields.pos_transactions.usecase.catalog.ProductUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    @Mock
    private ProductUseCase productUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createWithMultipartVariantsReturns201() throws Exception {
        Product saved = Product.builder().productId(1L).name("Coca Cola").active(true).variants(List.of()).build();
        when(productUseCase.create(any(), any(), any())).thenReturn(saved);

        MockMultipartFile product = new MockMultipartFile("product", "", "application/json",
                "{\"name\":\"Coca Cola\",\"categoryId\":1,\"brandId\":2}".getBytes());
        MockMultipartFile variants = new MockMultipartFile("variants", "", "application/json",
                "[{\"sku\":\"SKU-1\",\"barcode\":\"BAR-1\",\"price\":4500.00,\"costPrice\":3000.00,\"initialStock\":10}]".getBytes());

        mockMvc.perform(multipart("/api/products").file(product).file(variants))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("Coca Cola"));
    }

    // Note: @NotEmpty on the "variants" @RequestPart list is enforced via
    // Spring's method-validation AOP proxy (@Validated on the class), which
    // standalone MockMvc doesn't wire up without a full ApplicationContext —
    // verified instead via the live boot smoke test (see Fase 4 summary).

    @Test
    void listReturnsPagedContent() throws Exception {
        Product p = Product.builder().productId(1L).name("X").active(true).variants(List.of()).build();
        when(productUseCase.list(any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new PageResult<>(List.of(p), 1, 1, 0, 20));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listWithUnsortablePropertyReturns400() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/products?sort=stock,asc"))
                .andExpect(status().isBadRequest());
    }
}
