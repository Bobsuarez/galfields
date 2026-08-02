package co.com.galfields.pos_transactions.model.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class SaleItem {
    private Long saleItemId;
    private Long variantId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPerItem;
    private BigDecimal subtotal;
    private Long productUnitId;
    private String unitName;
    private Integer conversionFactor;
}
