package co.com.galfields.pos_transactions.model.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class VariantAttribute {
    private Long variantAttributeId;
    private String attributeName;
    private String attributeValue;
}
