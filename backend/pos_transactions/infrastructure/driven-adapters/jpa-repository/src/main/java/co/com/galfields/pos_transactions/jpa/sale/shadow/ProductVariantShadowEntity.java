package co.com.galfields.pos_transactions.jpa.sale.shadow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Read-only shadow of {@code product_variants}, scoped to existence checks
 * only — see EmployeeShadowEntity's javadoc. */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariantShadowEntity {

    @Id
    @Column(name = "variant_id")
    private Long variantId;
}
