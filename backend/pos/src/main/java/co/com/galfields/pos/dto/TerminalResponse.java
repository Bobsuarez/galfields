package co.com.galfields.pos.dto;

import java.time.LocalDateTime;

public record TerminalResponse(
        Long terminalId,
        String terminalCode,
        String name,
        boolean active,
        LocalDateTime createdAt) {
}
