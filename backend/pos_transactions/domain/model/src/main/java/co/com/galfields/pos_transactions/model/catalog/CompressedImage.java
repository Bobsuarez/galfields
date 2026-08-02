package co.com.galfields.pos_transactions.model.catalog;

/** Result of compressing an uploaded image: the encoded bytes plus the
 * content type/extension they were actually encoded as (may differ from the
 * original upload, e.g. a JPEG converted to WebP). */
public record CompressedImage(byte[] data, String contentType, String extension) {
}
