package co.com.galfields.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "invoice_numbering_ranges")
@Getter
@Setter
public class InvoiceNumberingRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "range_id")
    private Long rangeId;

    @Column(name = "terminal_code", nullable = false, unique = true, length = 50)
    private String terminalCode;

    @Column(nullable = false, length = 20)
    private String prefix;

    @Column(name = "range_start", nullable = false)
    private Long rangeStart;

    @Column(name = "range_end", nullable = false)
    private Long rangeEnd;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
