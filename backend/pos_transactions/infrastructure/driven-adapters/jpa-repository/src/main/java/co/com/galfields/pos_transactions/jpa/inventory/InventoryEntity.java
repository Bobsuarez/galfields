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

/** {@code last_updated} isn't mapped: DB-level DEFAULT/trigger
 * (trg_inventory_last_updated), no need to set it from Java. */
@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "location_id"}))
@Getter
@Setter
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;
}
