package co.com.galfields.pos_transactions.jpa.sale.shadow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Read-only shadow of {@code payment_methods}, scoped to existence checks
 * only — see EmployeeShadowEntity's javadoc. */
@Entity
@Table(name = "payment_methods")
@Getter
@Setter
public class PaymentMethodShadowEntity {

    @Id
    @Column(name = "payment_method_id")
    private Long paymentMethodId;
}
