package co.com.galfields.pos_transactions.jpa.sale.shadow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Read-only shadow of {@code locations}, scoped to resolving the default
 * location id by name — see EmployeeShadowEntity's javadoc. */
@Entity
@Table(name = "locations")
@Getter
@Setter
public class LocationShadowEntity {

    @Id
    @Column(name = "location_id")
    private Long locationId;

    @Column(nullable = false, length = 100)
    private String name;
}
