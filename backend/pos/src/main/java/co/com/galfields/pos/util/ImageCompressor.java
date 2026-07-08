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
 * Downscales and re-encodes uploaded images before they're stored, so large
 * phone/camera photos don't get persisted at full size. Images already
 * within the size cap are only quality-compressed (JPEG) or passed through
 * (PNG), never upscaled. Non-image or unrecognized content types pass
 * through untouched.
 */
@Component
public class ImageCompressor {

    private static final int MAX_DIMENSION = 1600;
    private static final float JPEG_QUALITY = 0.82f;

    public byte[] compress(MultipartFile file) {
        String formatName = formatNameOf(file.getContentType());
        if (formatName == null) {
            return readBytes(file);
        }

        try {
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                return readBytes(file);
            }

            boolean exceedsMaxDimension = original.getWidth() > MAX_DIMENSION || original.getHeight() > MAX_DIMENSION;
            var builder = exceedsMaxDimension
                    ? Thumbnails.of(original).size(MAX_DIMENSION, MAX_DIMENSION)
                    : Thumbnails.of(original).scale(1.0);
            builder.outputFormat(formatName);
            if ("jpg".equals(formatName)) {
                builder.outputQuality(JPEG_QUALITY);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new StorageException("Failed to compress image " + file.getOriginalFilename(), e);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new StorageException("Failed to read image " + file.getOriginalFilename(), e);
        }
    }

    private String formatNameOf(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            default -> null;
        };
    }
}
