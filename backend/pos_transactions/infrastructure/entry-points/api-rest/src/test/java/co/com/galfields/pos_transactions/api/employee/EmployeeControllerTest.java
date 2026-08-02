package co.com.galfields.pos_transactions.api.employee;

import co.com.galfields.pos_transactions.api.exception.GlobalExceptionHandler;
import co.com.galfields.pos_transactions.model.employee.Employee;
import co.com.galfields.pos_transactions.usecase.employee.EmployeeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeControllerTest {

    @Mock
    private EmployeeUseCase employeeUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new EmployeeController(employeeUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201AndNeverEchoesPassword() throws Exception {
        Employee saved = Employee.builder().employeeId(1L).firstName("Juan").lastName("Perez")
                .username("jperez").roleId(2L).roleName("Cajero").terminalIds(List.of()).active(true).build();
        when(employeeUseCase.create(any(), eq("secret123"))).thenReturn(saved);

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Juan","lastName":"Perez","username":"jperez","password":"secret123","roleId":2,"terminalIds":[]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.roleName").value("Cajero"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createWithoutPasswordReturns400() throws Exception {
        when(employeeUseCase.create(any(), isNull())).thenThrow(new IllegalArgumentException("password is required to create an employee"));

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Juan","lastName":"Perez","username":"jperez","roleId":2}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc.perform(delete("/api/employees/1")).andExpect(status().isNoContent());
    }
}
