package co.com.galfields.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "product_units")
@Getter
@Setter
public class ProductUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_unit_id")
    private Long productUnitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "unit_name", nullable = false, length = 50)
    private String unitName;

    @Column(name = "conversion_factor", nullable = false)
    private Integer conversionFactor;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(length = 100, unique = true)
    private String barcode;

    @Column(name = "is_base", nullable = false)
    private boolean base = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
