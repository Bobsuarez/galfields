package co.com.galfields.pos_transactions.jpa.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "variant_attributes", uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "attribute_name"}))
@Getter
@Setter
public class VariantAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_attribute_id")
    private Long variantAttributeId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "attribute_name", nullable = false, length = 50)
    private String attributeName;

    @Column(name = "attribute_value", nullable = false, length = 50)
    private String attributeValue;
}
