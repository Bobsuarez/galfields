package co.com.galfields.pos.dto;

import java.time.LocalDateTime;

public record InvoiceNumberingRangeResponse(
        Long rangeId,
        Long terminalId,
        String terminalCode,
        String prefix,
        Long rangeStart,
        Long rangeEnd,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
