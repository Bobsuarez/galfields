package co.com.galfields.pos_transactions.api.employee;

import java.time.LocalDateTime;

public record TerminalResponse(
        Long terminalId,
        String terminalCode,
        String name,
        boolean active,
        LocalDateTime createdAt) {
}
