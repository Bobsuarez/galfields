package co.com.galfields.pos.service;

import co.com.galfields.pos.config.MinioProperties;
import co.com.galfields.pos.entity.PaymentMethod;
import co.com.galfields.pos.entity.Product;
import co.com.galfields.pos.entity.ProductVariant;
import co.com.galfields.pos.exception.StorageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MinioService {

    private static final String UNCATEGORIZED_SLUG = "sin_categoria";
    private static final String OBJECT_KEY_PREFIX = "files";

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public String uploadProductImage(Product product, MultipartFile originalFile, byte[] imageData) {
        String folder = "%s/%s/%s".formatted(OBJECT_KEY_PREFIX, categorySlugOf(product), slugify(product.getName()));
        return upload(folder, originalFile, imageData);
    }

    public String uploadVariantImage(Product product, ProductVariant variant, MultipartFile originalFile, byte[] imageData) {
        String folder = "%s/%s/%s/variants/%s".formatted(
                OBJECT_KEY_PREFIX, categorySlugOf(product), slugify(product.getName()), slugify(variant.getSku()));
        return upload(folder, originalFile, imageData);
    }

    /**
     * files/payment_method/&lt;nombre-slug&gt;/&lt;uuid&gt;.ext
     */
    public String uploadPaymentMethodImage(PaymentMethod paymentMethod, MultipartFile originalFile, byte[] imageData) {
        String folder = "%s/payment_method/%s".formatted(OBJECT_KEY_PREFIX, slugify(paymentMethod.getMethodName()));
        return upload(folder, originalFile, imageData);
    }

    /**
     * Plain public URL, no signature/expiry - the bucket is served publicly
     * through the CDN, so this is just string concatenation, not a MinIO SDK
     * call. See the "Image URLs / CDN" note in CLAUDE.md.
     */
    public String getPublicUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        return minioProperties.publicEndpoint() + "/" + objectKey;
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to delete object " + objectKey, e);
        }
    }

    private String upload(String folder, MultipartFile originalFile, byte[] imageData) {
        String objectKey = "%s/%s%s".formatted(folder, UUID.randomUUID(), extensionOf(originalFile.getOriginalFilename()));
        try (var inputStream = new ByteArrayInputStream(imageData)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .stream(inputStream, imageData.length, -1)
                    .contentType(originalFile.getContentType())
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to upload image to MinIO", e);
        }
        return objectKey;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "";
    }

    private String categorySlugOf(Product product) {
        return Optional.ofNullable(product.getCategory())
                .map(category -> slugify(category.getName()))
                .orElse(UNCATEGORIZED_SLUG);
    }

    private String slugify(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "sin_nombre" : slug;
    }
}
