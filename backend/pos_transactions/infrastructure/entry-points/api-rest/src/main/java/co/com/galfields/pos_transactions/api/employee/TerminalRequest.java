package co.com.galfields.pos_transactions.api.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TerminalRequest(
        @NotBlank String terminalCode,
        String name,
        @NotNull Boolean active) {
}
