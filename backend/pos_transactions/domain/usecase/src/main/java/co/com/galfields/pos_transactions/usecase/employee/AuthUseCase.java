package co.com.galfields.pos_transactions.usecase.employee;

import co.com.galfields.pos_transactions.model.AuthenticationFailedException;
import co.com.galfields.pos_transactions.model.employee.Employee;
import co.com.galfields.pos_transactions.model.employee.EmployeeRole;
import co.com.galfields.pos_transactions.model.employee.LoginResult;
import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.model.employee.TokenClaims;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRoleRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.PasswordHasher;
import co.com.galfields.pos_transactions.model.employee.gateways.TerminalRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.TokenIssuer;
import lombok.RequiredArgsConstructor;

/**
 * Mirrors backend/pos's AuthService. Every failure mode (unknown username,
 * wrong password, inactive employee, role not allowed to log in on the
 * side attempted, unknown/unassigned terminal) returns the exact same
 * generic message — deliberately never reveals which check failed.
 */
@RequiredArgsConstructor
public class AuthUseCase {

    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final TerminalRepository terminalRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public LoginResult login(LoginCommand command) {
        Employee employee = employeeRepository.findByUsername(command.username())
                .filter(Employee::isActive)
                .orElseThrow(this::invalidCredentials);

        if (!passwordHasher.matches(command.password(), employee.getPasswordHash())) {
            throw invalidCredentials();
        }

        EmployeeRole role = employeeRoleRepository.findById(employee.getRoleId())
                .orElseThrow(this::invalidCredentials);

        Long terminalId = hasTerminalCode(command)
                ? resolveDesktopTerminalId(employee, role, command.terminalCode())
                : resolveMobileLogin(role);

        String token = tokenIssuer.issueToken(new TokenClaims(
                employee.getEmployeeId(), employee.getUsername(), role.getRoleId(), role.getRoleName(),
                role.getPermissions(), role.isCanLoginMobile(), role.isCanLoginDesktop(), terminalId));

        return new LoginResult(token, employee.getEmployeeId(), employee.getUsername(),
                role.getRoleId(), role.getRoleName(), role.getPermissions(), terminalId);
    }

    private boolean hasTerminalCode(LoginCommand command) {
        return command.terminalCode() != null && !command.terminalCode().isBlank();
    }

    private Long resolveDesktopTerminalId(Employee employee, EmployeeRole role, String terminalCode) {
        if (!role.isCanLoginDesktop()) {
            throw invalidCredentials();
        }
        Terminal terminal = terminalRepository.findByCode(terminalCode)
                .orElseThrow(this::invalidCredentials);
        boolean assigned = employee.getTerminalIds() != null
                && employee.getTerminalIds().contains(terminal.getTerminalId());
        if (!assigned) {
            throw invalidCredentials();
        }
        return terminal.getTerminalId();
    }

    private Long resolveMobileLogin(EmployeeRole role) {
        if (!role.isCanLoginMobile()) {
            throw invalidCredentials();
        }
        return null;
    }

    private AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("Usuario, clave o terminal inválidos");
    }
}
