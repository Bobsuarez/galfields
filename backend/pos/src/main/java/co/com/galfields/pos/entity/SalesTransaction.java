package co.com.galfields.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sales_transactions")
@Getter
@Setter
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    /**
     * The actual moment the sale happened (UTC) - set explicitly by
     * SalesService.recordSale from the reporting terminal's transactionDate
     * when present, or the moment the cloud received the report otherwise.
     * Deliberately NOT {@code @CreationTimestamp} - that annotation always
     * overwrites whatever value is set at persist time, which would make it
     * impossible to honor the terminal's own timestamp. See
     * specs/01-reportes-mobile-pos-zona-horaria.md.
     */
    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_status", nullable = false, columnDefinition = "payment_status_enum")
    private PaymentStatus paymentStatus = PaymentStatus.Pending;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /** Idempotency key from the reporting POS terminal - see SalesService. */
    @Column(name = "client_event_id", unique = true, length = 100)
    private String clientEventId;

    /** Separate axis from paymentStatus - a Paid sale that gets voided is
     * still "was Paid", just cancelled now. Null means active. */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** DIAN-authorized invoice number snapshotted by the reporting terminal
     * at sale creation - see apps/galfield-pos's invoices.rs::create_sale.
     * Null for transactions reported before this column existed (see
     * V8__sales_invoice_number.sql); callers fall back to transactionId. */
    @Column(name = "invoice_prefix", length = 20)
    private String invoicePrefix;

    @Column(name = "invoice_number", length = 30)
    private String invoiceNumber;
}
