package co.com.galfields.pos_transactions.model.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class StockAdjustment {
    private Long adjustmentId;
    private String clientEventId;
    private Long variantId;
    private Long locationId;
    private Integer quantityDelta;
    private Integer resultingQuantity;
}
