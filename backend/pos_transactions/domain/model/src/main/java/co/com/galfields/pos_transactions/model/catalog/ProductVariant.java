package co.com.galfields.pos_transactions.model.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProductVariant {
    private Long variantId;
    private String sku;
    private String barcode;
    private BigDecimal price;
    private BigDecimal costPrice;
    private boolean active;
    /** Initial/desired stock on create/update — write-only input, never returned. */
    private Integer initialStock;
    /** Resolved current stock (from Inventory) — read-only output, not persisted here. */
    private Integer stock;
    private String imageObjectKey;
    /** Set alongside imageObjectKey only when a new image is uploaded this
     * call — needed by the adapter for the attach_files row. */
    private String imageMimeType;
    private Integer imageSize;
    private String imageUrl;
    private List<VariantAttribute> attributes;
    private List<ProductUnit> units;
}
