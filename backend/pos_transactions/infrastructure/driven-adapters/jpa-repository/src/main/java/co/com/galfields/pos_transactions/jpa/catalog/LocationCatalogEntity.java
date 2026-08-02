package co.com.galfields.pos_transactions.jpa.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** The real, full-CRUD owner of {@code locations} (Fase 4) — coexists with
 * Sale's/Inventory's own private read-only shadow entities over the same
 * table (jpa.sale.shadow.LocationShadowEntity, jpa.inventory.LocationRefEntity). */
@Entity
@Table(name = "locations")
@Getter
@Setter
public class LocationCatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long locationId;

    @Column(nullable = false, length = 100)
    private String name;

    private String address;

    @Column(length = 20)
    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
