package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        // Omitted -> mobile login (requires canLoginMobile). Present ->
        // desktop login (requires canLoginDesktop + an employee_terminals
        // assignment for this terminal).
        String terminalCode) {
}
