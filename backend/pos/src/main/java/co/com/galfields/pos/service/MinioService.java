package co.com.galfields.pos.service;

import co.com.galfields.pos.config.MinioProperties;
import co.com.galfields.pos.entity.Product;
import co.com.galfields.pos.exception.StorageException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
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

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public String uploadProductImage(Product product, MultipartFile file) {
        String objectKey = buildObjectKey(product, file.getOriginalFilename());
        try (var inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to upload image to MinIO for product " + product.getProductId(), e);
        }
        return objectKey;
    }

    public String getPresignedUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .expiry((int) minioProperties.presignedUrlExpirySeconds())
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned URL for object " + objectKey, e);
        }
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

    private String buildObjectKey(Product product, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String categorySlug = Optional.ofNullable(product.getCategory())
                .map(category -> slugify(category.getName()))
                .orElse(UNCATEGORIZED_SLUG);
        String productSlug = slugify(product.getName());
        return "%s/%s/%s%s".formatted(categorySlug, productSlug, UUID.randomUUID(), extension);
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
