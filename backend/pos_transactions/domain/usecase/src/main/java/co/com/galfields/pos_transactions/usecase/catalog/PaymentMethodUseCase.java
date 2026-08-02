package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.CompressedImage;
import co.com.galfields.pos_transactions.model.catalog.PaymentMethod;
import co.com.galfields.pos_transactions.model.catalog.Slug;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageCompressionGateway;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageStorageGateway;
import co.com.galfields.pos_transactions.model.catalog.gateways.PaymentMethodCatalogRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Mirrors backend/pos's PaymentMethodService. Unlike products, payment
 * methods hard-delete (deletePaymentMethod also cleans up the MinIO object,
 * since there's no soft-delete path that would otherwise leave it orphaned).
 */
@RequiredArgsConstructor
public class PaymentMethodUseCase {

    private final PaymentMethodCatalogRepository paymentMethodRepository;
    private final ImageCompressionGateway imageCompressionGateway;
    private final ImageStorageGateway imageStorageGateway;

    public PaymentMethod create(PaymentMethod paymentMethod, UploadedImage image) {
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        if (image != null) {
            saved = attachImage(saved, image);
        }
        return withResolvedImageUrl(saved);
    }

    public PaymentMethod get(Long paymentMethodId) {
        return withResolvedImageUrl(findOrThrow(paymentMethodId));
    }

    public List<PaymentMethod> list() {
        return paymentMethodRepository.findAllOrderByMethodName().stream()
                .map(this::withResolvedImageUrl)
                .toList();
    }

    public PaymentMethod update(Long paymentMethodId, PaymentMethod patch, UploadedImage image) {
        PaymentMethod existing = findOrThrow(paymentMethodId);
        existing.setMethodName(patch.getMethodName());
        existing.setActive(patch.isActive());
        PaymentMethod saved = paymentMethodRepository.save(existing);

        if (image != null) {
            saved = attachImage(saved, image);
        }
        return withResolvedImageUrl(saved);
    }

    public void delete(Long paymentMethodId) {
        PaymentMethod paymentMethod = findOrThrow(paymentMethodId);
        String objectKey = paymentMethod.getImageObjectKey();
        paymentMethodRepository.deleteById(paymentMethodId);
        if (objectKey != null) {
            imageStorageGateway.delete(objectKey);
        }
    }

    private PaymentMethod attachImage(PaymentMethod paymentMethod, UploadedImage image) {
        CompressedImage compressed = imageCompressionGateway.compress(
                image.data(), image.contentType(), image.originalFilename());
        String folder = "files/payment_method/%s".formatted(Slug.of(paymentMethod.getMethodName()));
        String objectKey = imageStorageGateway.upload(folder, compressed);

        String previousObjectKey = paymentMethod.getImageObjectKey();
        paymentMethod.setImageObjectKey(objectKey);
        paymentMethod.setImageMimeType(compressed.contentType());
        paymentMethod.setImageSize(compressed.data().length);
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        if (previousObjectKey != null) {
            imageStorageGateway.delete(previousObjectKey);
        }
        return saved;
    }

    private PaymentMethod withResolvedImageUrl(PaymentMethod paymentMethod) {
        paymentMethod.setImageUrl(imageStorageGateway.getPublicUrl(paymentMethod.getImageObjectKey()));
        return paymentMethod;
    }

    private PaymentMethod findOrThrow(Long paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod " + paymentMethodId + " not found"));
    }

    public record UploadedImage(byte[] data, String contentType, String originalFilename) {
    }
}
