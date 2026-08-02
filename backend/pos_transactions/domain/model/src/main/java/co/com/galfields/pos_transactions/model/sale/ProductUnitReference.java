package co.com.galfields.pos_transactions.model.sale;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductUnitReference {
    private final String unitName;
    private final Integer conversionFactor;
}
