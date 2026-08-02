package co.com.galfields.pos_transactions.model.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProductUnit {
    private Long productUnitId;
    private Long variantId;
    private String unitName;
    private Integer conversionFactor;
    private BigDecimal unitPrice;
    private String barcode;
    private boolean base;
    private boolean active;
    private LocalDateTime createdAt;
    /** variantStock / conversionFactor (floorDiv) — resolved by the usecase, not persisted. */
    private Integer stock;
}
