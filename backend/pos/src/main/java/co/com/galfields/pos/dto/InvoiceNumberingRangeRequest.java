package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceNumberingRangeRequest(
        @NotNull Long terminalId,
        @NotBlank String prefix,
        @NotNull Long rangeStart,
        @NotNull Long rangeEnd) {
}
