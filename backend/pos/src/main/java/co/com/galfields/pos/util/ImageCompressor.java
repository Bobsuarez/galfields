package co.com.galfields.pos.util;

import co.com.galfields.pos.exception.StorageException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Downscales and re-encodes uploaded raster images (JPEG/PNG) to WebP before
 * they're stored, so large phone/camera photos don't get persisted at full
 * size or in a heavier format. WebP beats JPEG/PNG on both file size and
 * decode speed at equivalent visual quality, which is what actually moves
 * catalog load times on the CDN. Images already within the size cap are
 * still re-encoded to WebP, never upscaled. Non-raster or unrecognized
 * content types (e.g. GIF, SVG) pass through untouched.
 */
@Component
public class ImageCompressor {

    private static final int MAX_DIMENSION = 1600;
    private static final float WEBP_QUALITY = 0.80f;
    private static final String TARGET_FORMAT = "webp";
    private static final String TARGET_CONTENT_TYPE = "image/webp";
    private static final String TARGET_EXTENSION = ".webp";

    public CompressedImage compress(MultipartFile file) {
        if (!isConvertible(file.getContentType())) {
            return passThrough(file);
        }

        try {
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                return passThrough(file);
            }

            boolean exceedsMaxDimension = original.getWidth() > MAX_DIMENSION || original.getHeight() > MAX_DIMENSION;
            var builder = exceedsMaxDimension
                    ? Thumbnails.of(original).size(MAX_DIMENSION, MAX_DIMENSION)
                    : Thumbnails.of(original).scale(1.0);
            builder.outputFormat(TARGET_FORMAT)
                    .outputQuality(WEBP_QUALITY);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toOutputStream(out);
            return new CompressedImage(out.toByteArray(), TARGET_CONTENT_TYPE, TARGET_EXTENSION);
        } catch (IOException e) {
            throw new StorageException("Failed to compress image " + file.getOriginalFilename(), e);
        }
    }

    private CompressedImage passThrough(MultipartFile file) {
        return new CompressedImage(readBytes(file), file.getContentType(), extensionOf(file.getOriginalFilename()));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new StorageException("Failed to read image " + file.getOriginalFilename(), e);
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
