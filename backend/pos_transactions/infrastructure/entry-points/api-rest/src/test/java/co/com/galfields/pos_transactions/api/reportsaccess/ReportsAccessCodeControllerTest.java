package co.com.galfields.pos_transactions.api.reportsaccess;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.reportsaccess.ReportsAccessCode;
import co.com.galfields.pos_transactions.usecase.reportsaccess.ReportsAccessCodeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportsAccessCodeControllerTest {

    @Mock
    private ReportsAccessCodeUseCase reportsAccessCodeUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportsAccessCodeController(reportsAccessCodeUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void generateReturns201WithCode() throws Exception {
        when(reportsAccessCodeUseCase.generate()).thenReturn(
                ReportsAccessCode.builder().code("123456").generatedAt(LocalDateTime.now()).build());

        mockMvc.perform(post("/api/reports-access-code"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("123456"));
    }

    @Test
    void validateReturnsTrueForMatchingCode() throws Exception {
        when(reportsAccessCodeUseCase.validate("123456")).thenReturn(true);

        mockMvc.perform(post("/api/reports-access-code/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validateReturnsFalseForWrongCode() throws Exception {
        when(reportsAccessCodeUseCase.validate("000000")).thenReturn(false);

        mockMvc.perform(post("/api/reports-access-code/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void validateWithBlankCodeReturns400() throws Exception {
        mockMvc.perform(post("/api/reports-access-code/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
