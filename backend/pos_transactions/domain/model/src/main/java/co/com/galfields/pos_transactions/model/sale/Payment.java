package co.com.galfields.pos_transactions.model.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Payment {
    private Long paymentId;
    private Long paymentMethodId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String referenceNumber;
}
