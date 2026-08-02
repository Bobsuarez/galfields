package co.com.galfields.pos_transactions.jpa.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Read-only shadow of {@code locations}, scoped to resolving the default
 * location id by name — own copy for the Inventario module, same shape as
 * jpa.sale.shadow.LocationShadowEntity (Fase 2); both coexist on the same
 * table, see spec 04's Fase 2/3 coordination note. */
@Entity
@Table(name = "locations")
@Getter
@Setter
public class LocationRefEntity {

    @Id
    @Column(name = "location_id")
    private Long locationId;

    @Column(nullable = false, length = 100)
    private String name;
}
