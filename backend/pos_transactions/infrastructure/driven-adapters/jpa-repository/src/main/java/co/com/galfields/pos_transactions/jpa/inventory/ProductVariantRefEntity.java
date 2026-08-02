package co.com.galfields.pos_transactions.jpa.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Read-only shadow of {@code product_variants}, scoped to existence checks
 * only — own copy for the Inventario module, see LocationRefEntity's javadoc. */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariantRefEntity {

    @Id
    @Column(name = "variant_id")
    private Long variantId;
}
