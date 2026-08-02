package co.com.galfields.pos_transactions.api.auth;

import co.com.galfields.pos_transactions.model.employee.LoginResult;
import co.com.galfields.pos_transactions.usecase.employee.AuthUseCase;
import co.com.galfields.pos_transactions.usecase.employee.LoginCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors backend/pos's AuthController — public per SecurityConfig. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = authUseCase.login(new LoginCommand(request.username(), request.password(), request.terminalCode()));
        return new LoginResponse(result.token(), result.employeeId(), result.username(), result.roleId(),
                result.roleName(), result.permissions(), result.terminalId());
    }
}
