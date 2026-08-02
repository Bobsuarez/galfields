package co.com.galfields.pos_transactions.model.invoicing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class InvoiceNumberingRange {
    private Long rangeId;
    private Long terminalId;
    /** Denormalized, resolved by the usecase — not a persisted column. */
    private String terminalCode;
    private String prefix;
    private Long rangeStart;
    private Long rangeEnd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
