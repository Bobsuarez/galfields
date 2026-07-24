package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceNumberingRangeRequest(
        @NotBlank String terminalCode,
        @NotBlank String prefix,
        @NotNull Long rangeStart,
        @NotNull Long rangeEnd) {
}
