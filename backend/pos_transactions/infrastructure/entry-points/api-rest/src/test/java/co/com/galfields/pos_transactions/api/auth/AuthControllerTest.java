package co.com.galfields.pos_transactions.api.auth;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.AuthenticationFailedException;
import co.com.galfields.pos_transactions.model.employee.LoginResult;
import co.com.galfields.pos_transactions.usecase.employee.AuthUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    @Mock
    private AuthUseCase authUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsTokenAndClaims() throws Exception {
        when(authUseCase.login(any())).thenReturn(new LoginResult(
                "jwt-token", 1L, "cajero1", 2L, "Cajero", Map.of("pos", true), 5L));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cajero1\",\"password\":\"secret\",\"terminalCode\":\"T1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.terminalId").value(5));
    }

    @Test
    void invalidCredentialsReturns401() throws Exception {
        when(authUseCase.login(any())).thenThrow(new AuthenticationFailedException("Usuario, clave o terminal inválidos"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cajero1\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingUsernameReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest());
    }
}
