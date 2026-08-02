package co.com.galfields.pos_transactions.jpa.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** {@code created_at} isn't mapped: DB-level DEFAULT CURRENT_TIMESTAMP,
 * no need to set it from Java. */
@Entity
@Table(name = "stock_adjustments", uniqueConstraints = @UniqueConstraint(columnNames = {"client_event_id", "variant_id"}))
@Getter
@Setter
public class StockAdjustmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adjustment_id")
    private Long adjustmentId;

    @Column(name = "client_event_id", nullable = false, length = 100)
    private String clientEventId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    @Column(name = "resulting_quantity", nullable = false)
    private Integer resultingQuantity;
}
