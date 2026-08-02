package co.com.galfields.pos_transactions.api.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        /** Omitted -> mobile login. Present -> desktop login. */
        String terminalCode) {
}
