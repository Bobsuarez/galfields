package co.com.galfields.pos_transactions.minio;

import co.com.galfields.pos_transactions.model.StorageException;
import co.com.galfields.pos_transactions.model.catalog.CompressedImage;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageCompressionGateway;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Mirrors backend/pos's ImageCompressor: downscales to a 1600px max
 * dimension (never upscales) and re-encodes JPEG/PNG uploads to WebP before
 * they're stored. Non-raster/unrecognized content types pass through
 * untouched.
 */
@Component
public class ThumbnailatorImageCompressionAdapter implements ImageCompressionGateway {

    private static final int MAX_DIMENSION = 1600;
    private static final float WEBP_QUALITY = 0.80f;
    private static final String TARGET_FORMAT = "webp";
    private static final String TARGET_CONTENT_TYPE = "image/webp";
    private static final String TARGET_EXTENSION = ".webp";

    @Override
    public CompressedImage compress(byte[] data, String contentType, String originalFilename) {
        if (!isConvertible(contentType)) {
            return new CompressedImage(data, contentType, extensionOf(originalFilename));
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(data));
            if (original == null) {
                return new CompressedImage(data, contentType, extensionOf(originalFilename));
            }

            boolean exceedsMaxDimension = original.getWidth() > MAX_DIMENSION || original.getHeight() > MAX_DIMENSION;
            var builder = exceedsMaxDimension
                    ? Thumbnails.of(original).size(MAX_DIMENSION, MAX_DIMENSION)
                    : Thumbnails.of(original).scale(1.0);
            builder.outputFormat(TARGET_FORMAT).outputQuality(WEBP_QUALITY);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toOutputStream(out);
            return new CompressedImage(out.toByteArray(), TARGET_CONTENT_TYPE, TARGET_EXTENSION);
        } catch (IOException e) {
            throw new StorageException("Failed to compress image " + originalFilename, e);
        }
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "";
    }

    private boolean isConvertible(String contentType) {
        return "image/jpeg".equals(contentType)
                || "image/jpg".equals(contentType)
                || "image/png".equals(contentType);
    }
}
