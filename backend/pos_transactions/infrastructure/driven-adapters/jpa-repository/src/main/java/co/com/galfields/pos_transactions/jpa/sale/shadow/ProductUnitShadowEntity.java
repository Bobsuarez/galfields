package co.com.galfields.pos_transactions.jpa.sale.shadow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Read-only shadow of {@code product_units}, scoped to resolving the
 * unit name/conversion factor a sale line was sold under — see
 * EmployeeShadowEntity's javadoc. */
@Entity
@Table(name = "product_units")
@Getter
@Setter
public class ProductUnitShadowEntity {

    @Id
    @Column(name = "product_unit_id")
    private Long productUnitId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "unit_name", nullable = false, length = 50)
    private String unitName;

    @Column(name = "conversion_factor", nullable = false)
    private Integer conversionFactor;
}
