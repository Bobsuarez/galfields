package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.LoginRequest;
import co.com.galfields.pos.dto.LoginResponse;
import co.com.galfields.pos.entity.Employee;
import co.com.galfields.pos.entity.EmployeeRole;
import co.com.galfields.pos.entity.Terminal;
import co.com.galfields.pos.exception.AuthenticationFailedException;
import co.com.galfields.pos.repository.EmployeeRepository;
import co.com.galfields.pos.repository.TerminalRepository;
import co.com.galfields.pos.util.PermissionsJson;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final TerminalRepository terminalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByUsername(request.username())
                .filter(Employee::isActive)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw invalidCredentials();
        }

        EmployeeRole role = employee.getRole();
        Long terminalId = hasTerminalCode(request)
                ? resolveDesktopTerminalId(employee, role, request.terminalCode())
                : resolveMobileLogin(role);

        Map<String, Boolean> permissions = PermissionsJson.read(objectMapper, role.getPermissions());
        String token = jwtService.issueToken(employee, terminalId, permissions);

        return new LoginResponse(
                token,
                employee.getEmployeeId(),
                employee.getUsername(),
                role.getRoleId(),
                role.getRoleName(),
                permissions,
                terminalId);
    }

    private boolean hasTerminalCode(LoginRequest request) {
        return request.terminalCode() != null && !request.terminalCode().isBlank();
    }

    // Desktop login: role must be allowed to log into the desktop, and the
    // employee must actually be assigned to this specific terminal.
    private Long resolveDesktopTerminalId(Employee employee, EmployeeRole role, String terminalCode) {
        if (!role.isCanLoginDesktop()) {
            throw invalidCredentials();
        }
        Terminal terminal = terminalRepository.findByTerminalCode(terminalCode)
                .orElseThrow(this::invalidCredentials);
        boolean assigned = employee.getTerminals().stream()
                .anyMatch(t -> t.getTerminalId().equals(terminal.getTerminalId()));
        if (!assigned) {
            throw invalidCredentials();
        }
        return terminal.getTerminalId();
    }

    // Mobile login: no terminalCode was sent, so this only needs the
    // mobile login flag - no terminal claim on the resulting JWT.
    private Long resolveMobileLogin(EmployeeRole role) {
        if (!role.isCanLoginMobile()) {
            throw invalidCredentials();
        }
        return null;
    }

    // Deliberately the same generic message for every failure mode (unknown
    // username, wrong password, inactive employee, role not allowed to log
    // in here, unknown/unassigned terminal) - never reveal which part failed.
    private AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("Usuario, clave o terminal inválidos");
    }
}
