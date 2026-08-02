package co.com.galfields.pos_transactions.model.catalog.gateways;

import co.com.galfields.pos_transactions.model.catalog.CompressedImage;

public interface ImageStorageGateway {
    /** Uploads under a caller-built folder path (business-naming logic stays
     * in the usecase — this port is technology-only) and returns the object key. */
    String upload(String folder, CompressedImage image);

    /** Plain, unsigned public URL — see backend/pos's "Image URLs / CDN" note:
     * the bucket is served publicly through the CDN, so this is string
     * concatenation, not a signed URL. */
    String getPublicUrl(String objectKey);

    void delete(String objectKey);
}
