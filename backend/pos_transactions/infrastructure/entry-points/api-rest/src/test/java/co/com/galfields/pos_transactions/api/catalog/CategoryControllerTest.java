package co.com.galfields.pos_transactions.api.catalog;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.Category;
import co.com.galfields.pos_transactions.usecase.catalog.CategoryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerTest {

    @Mock
    private CategoryUseCase categoryUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryController(categoryUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201() throws Exception {
        when(categoryUseCase.create(any())).thenReturn(Category.builder().categoryId(1L).name("Bebidas").build());

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bebidas\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"categoryId\":1,\"name\":\"Bebidas\"}"));
    }

    @Test
    void createWithoutNameReturns400() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownReturns404() throws Exception {
        when(categoryUseCase.get(anyLong())).thenThrow(new ResourceNotFoundException("Category 404 not found"));

        mockMvc.perform(get("/api/categories/404")).andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1")).andExpect(status().isNoContent());
    }
}
