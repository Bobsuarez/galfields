package co.com.galfields.pos_transactions.model.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Product {
    private Long productId;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private String imageObjectKey;
    /** Set alongside imageObjectKey only when a new image is uploaded this
     * call — needed by the adapter for the attach_files row. */
    private String imageMimeType;
    private Integer imageSize;
    private String imageUrl;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductVariant> variants;
}
