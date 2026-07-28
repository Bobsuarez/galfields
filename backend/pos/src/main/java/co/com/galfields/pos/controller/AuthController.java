package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.LoginRequest;
import co.com.galfields.pos.dto.LoginResponse;
import co.com.galfields.pos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Public per this spec's SecurityFilterChain (step 7) - until then, on
    // the classpath-security-added-but-not-configured window, this endpoint
    // is still locked behind Spring Boot's default HTTP Basic like every
    // other endpoint (see CLAUDE.md's "Dependencies of note").
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }
}
