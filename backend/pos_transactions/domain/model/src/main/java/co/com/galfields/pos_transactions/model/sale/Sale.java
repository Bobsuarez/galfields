package co.com.galfields.pos_transactions.model.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Sale {
    private Long transactionId;
    private LocalDateTime transactionDate;
    private Long employeeId;
    private Long locationId;
    private Long customerId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private PaymentStatus paymentStatus;
    private String clientEventId;
    private LocalDateTime cancelledAt;
    private String invoicePrefix;
    private String invoiceNumber;
    private List<SaleItem> items;
    private List<Payment> payments;
}
