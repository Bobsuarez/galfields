package co.com.galfields.pos_transactions.jpa.sale;

import co.com.galfields.pos_transactions.model.sale.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_transactions")
@Getter
@Setter
public class SalesTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @CreationTimestamp
    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_status", nullable = false, columnDefinition = "payment_status_enum")
    private PaymentStatus paymentStatus;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "client_event_id", unique = true, length = 100)
    private String clientEventId;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "invoice_prefix", length = 20)
    private String invoicePrefix;

    @Column(name = "invoice_number", length = 30)
    private String invoiceNumber;
}
