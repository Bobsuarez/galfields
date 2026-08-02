package co.com.galfields.pos_transactions.minio;

import co.com.galfields.pos_transactions.model.StorageException;
import co.com.galfields.pos_transactions.model.catalog.CompressedImage;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageStorageGateway;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/** Mirrors backend/pos's MinioService — technology-only: folder-path/naming
 * business logic (category slug, product/variant name) stays in the
 * usecase layer, this just uploads under the folder it's given. */
@Component
@RequiredArgsConstructor
public class MinioImageStorageAdapter implements ImageStorageGateway {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String upload(String folder, CompressedImage image) {
        String objectKey = "%s/%s%s".formatted(folder, UUID.randomUUID(), image.extension());
        try (var inputStream = new ByteArrayInputStream(image.data())) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .stream(inputStream, image.data().length, -1)
                    .contentType(image.contentType())
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to upload image to MinIO", e);
        }
        return objectKey;
    }

    @Override
    public String getPublicUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        return minioProperties.publicEndpoint() + "/" + objectKey;
    }

    @Override
    public void delete(String objectKey) {
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
}
