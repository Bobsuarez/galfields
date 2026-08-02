package co.com.galfields.pos_transactions.usecase.employee;

import co.com.galfields.pos_transactions.model.AuthenticationFailedException;
import co.com.galfields.pos_transactions.model.employee.Employee;
import co.com.galfields.pos_transactions.model.employee.EmployeeRole;
import co.com.galfields.pos_transactions.model.employee.LoginResult;
import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRoleRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.PasswordHasher;
import co.com.galfields.pos_transactions.model.employee.gateways.TerminalRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeRoleRepository employeeRoleRepository;
    @Mock
    private TerminalRepository terminalRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private TokenIssuer tokenIssuer;

    private AuthUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new AuthUseCase(employeeRepository, employeeRoleRepository, terminalRepository, passwordHasher, tokenIssuer);
    }

    private Employee activeEmployee() {
        return Employee.builder().employeeId(1L).username("cajero1").passwordHash("hash")
                .roleId(2L).active(true).terminalIds(List.of(5L)).build();
    }

    @Test
    void mobileLoginSucceedsWhenRoleAllowsIt() {
        when(employeeRepository.findByUsername("cajero1")).thenReturn(Optional.of(activeEmployee()));
        when(passwordHasher.matches("secret", "hash")).thenReturn(true);
        EmployeeRole role = EmployeeRole.builder().roleId(2L).roleName("Administrador")
                .permissions(Map.of("pos", true)).canLoginMobile(true).canLoginDesktop(false).build();
        when(employeeRoleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(tokenIssuer.issueToken(any())).thenReturn("jwt-token");

        LoginResult result = useCase.login(new LoginCommand("cajero1", "secret", null));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.terminalId()).isNull();
        assertThat(result.roleName()).isEqualTo("Administrador");
    }

    @Test
    void mobileLoginRejectedWhenRoleCannotLoginMobile() {
        when(employeeRepository.findByUsername("cajero1")).thenReturn(Optional.of(activeEmployee()));
        when(passwordHasher.matches("secret", "hash")).thenReturn(true);
        EmployeeRole role = EmployeeRole.builder().roleId(2L).roleName("Cajero")
                .permissions(Map.of()).canLoginMobile(false).canLoginDesktop(true).build();
        when(employeeRoleRepository.findById(2L)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> useCase.login(new LoginCommand("cajero1", "secret", null)))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void desktopLoginSucceedsWhenAssignedToTerminal() {
        when(employeeRepository.findByUsername("cajero1")).thenReturn(Optional.of(activeEmployee()));
        when(passwordHasher.matches("secret", "hash")).thenReturn(true);
        EmployeeRole role = EmployeeRole.builder().roleId(2L).roleName("Cajero")
                .permissions(Map.of("pos", true)).canLoginMobile(false).canLoginDesktop(true).build();
        when(employeeRoleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(terminalRepository.findByCode("T1")).thenReturn(Optional.of(Terminal.builder().terminalId(5L).terminalCode("T1").build()));
        when(tokenIssuer.issueToken(any())).thenReturn("jwt-token");

        LoginResult result = useCase.login(new LoginCommand("cajero1", "secret", "T1"));

        assertThat(result.terminalId()).isEqualTo(5L);
    }

    @Test
    void desktopLoginRejectedWhenEmployeeNotAssignedToTerminal() {
        when(employeeRepository.findByUsername("cajero1")).thenReturn(Optional.of(activeEmployee()));
        when(passwordHasher.matches("secret", "hash")).thenReturn(true);
        EmployeeRole role = EmployeeRole.builder().roleId(2L).canLoginDesktop(true).build();
        when(employeeRoleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(terminalRepository.findByCode("T2")).thenReturn(Optional.of(Terminal.builder().terminalId(99L).terminalCode("T2").build()));

        assertThatThrownBy(() -> useCase.login(new LoginCommand("cajero1", "secret", "T2")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void wrongPasswordRejected() {
        when(employeeRepository.findByUsername("cajero1")).thenReturn(Optional.of(activeEmployee()));
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.login(new LoginCommand("cajero1", "wrong", null)))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void unknownUsernameRejectedWithSameGenericMessage() {
        when(employeeRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.login(new LoginCommand("ghost", "whatever", null)))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuario, clave o terminal inválidos");
    }

    @Test
    void inactiveEmployeeRejected() {
        Employee inactive = activeEmployee().toBuilder().active(false).build();
        when(employeeRepository.findByUsername("cajero1")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> useCase.login(new LoginCommand("cajero1", "secret", null)))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
