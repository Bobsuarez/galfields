package co.com.galfields.pos_transactions.usecase.employee;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.employee.Employee;
import co.com.galfields.pos_transactions.model.employee.EmployeeRole;
import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRoleRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.PasswordHasher;
import co.com.galfields.pos_transactions.model.employee.gateways.TerminalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeRoleRepository employeeRoleRepository;
    @Mock
    private TerminalRepository terminalRepository;
    @Mock
    private PasswordHasher passwordHasher;

    private EmployeeUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new EmployeeUseCase(employeeRepository, employeeRoleRepository, terminalRepository, passwordHasher);
    }

    @Test
    void createRequiresPassword() {
        Employee employee = Employee.builder().roleId(1L).terminalIds(List.of()).build();

        assertThatThrownBy(() -> useCase.create(employee, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.create(employee, "  ")).isInstanceOf(IllegalArgumentException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createHashesPasswordAndResolvesRoleAndTerminals() {
        Employee employee = Employee.builder().roleId(1L).terminalIds(List.of(5L)).build();
        when(passwordHasher.hash("secret")).thenReturn("hashed");
        when(employeeRoleRepository.findById(1L)).thenReturn(Optional.of(EmployeeRole.builder().roleId(1L).roleName("Cajero").build()));
        when(terminalRepository.findAllExisting(List.of(5L))).thenReturn(List.of(Terminal.builder().terminalId(5L).build()));
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee result = useCase.create(employee, "secret");

        assertThat(result.getPasswordHash()).isEqualTo("hashed");
        assertThat(result.getRoleName()).isEqualTo("Cajero");
    }

    @Test
    void createThrowsWhenTerminalIdsDoNotAllExist() {
        Employee employee = Employee.builder().roleId(1L).terminalIds(List.of(5L, 6L)).build();
        when(employeeRoleRepository.findById(1L)).thenReturn(Optional.of(EmployeeRole.builder().roleId(1L).build()));
        when(terminalRepository.findAllExisting(List.of(5L, 6L))).thenReturn(List.of(Terminal.builder().terminalId(5L).build()));

        assertThatThrownBy(() -> useCase.create(employee, "secret")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateWithBlankPasswordLeavesHashUntouched() {
        Employee existing = Employee.builder().employeeId(1L).passwordHash("original-hash").roleId(1L).terminalIds(List.of()).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRoleRepository.findById(1L)).thenReturn(Optional.of(EmployeeRole.builder().roleId(1L).roleName("Cajero").build()));
        when(terminalRepository.findAllExisting(List.of())).thenReturn(List.of());
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee patch = Employee.builder().firstName("New").roleId(1L).terminalIds(List.of()).build();
        Employee result = useCase.update(1L, patch, "  ");

        assertThat(result.getPasswordHash()).isEqualTo("original-hash");
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    void deactivateFlipsActiveFalse() {
        Employee existing = Employee.builder().employeeId(1L).active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any())).thenReturn(existing);

        useCase.deactivate(1L);

        assertThat(existing.isActive()).isFalse();
    }
}
