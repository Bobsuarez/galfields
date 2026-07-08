package co.com.galfields.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "variant_attributes", uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "attribute_name"}))
@Getter
@Setter
public class VariantAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_attribute_id")
    private Long variantAttributeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "attribute_name", nullable = false, length = 50)
    private String attributeName;

    @Column(name = "attribute_value", nullable = false, length = 50)
    private String attributeValue;
}
