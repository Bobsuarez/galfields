package co.com.galfields.pos_transactions.model.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class PaymentMethod {
    private Long paymentMethodId;
    private String methodName;
    private boolean active;
    /** MinIO object key persisted with the row — null until an image is attached. */
    private String imageObjectKey;
    /** Set alongside imageObjectKey when a new image is uploaded — needed by
     * the adapter to persist the attach_files row (mime_type/size are NOT
     * NULL columns). Null on a save that doesn't change the image. */
    private String imageMimeType;
    private Integer imageSize;
    /** Resolved public URL, filled in by the usecase before returning to the API layer — not persisted. */
    private String imageUrl;
    private LocalDateTime createdAt;
}
