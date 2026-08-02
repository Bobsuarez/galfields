package co.com.galfields.pos_transactions.jpa.catalog;

import co.com.galfields.pos_transactions.model.catalog.PaymentMethod;
import co.com.galfields.pos_transactions.model.catalog.gateways.PaymentMethodCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentMethodRepositoryAdapter implements PaymentMethodCatalogRepository {

    private final PaymentMethodJpaRepository repository;
    private final PaymentMethodImageJpaRepository imageRepository;
    private final AttachFileJpaRepository attachFileRepository;

    @Override
    public Optional<PaymentMethod> findById(Long paymentMethodId) {
        return repository.findById(paymentMethodId).map(this::toDomain);
    }

    @Override
    public List<PaymentMethod> findAllOrderByMethodName() {
        return repository.findAllByOrderByMethodNameAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public PaymentMethod save(PaymentMethod paymentMethod) {
        PaymentMethodEntity entity = new PaymentMethodEntity();
        entity.setPaymentMethodId(paymentMethod.getPaymentMethodId());
        entity.setMethodName(paymentMethod.getMethodName());
        entity.setActive(paymentMethod.isActive());
        // See CategoryRepositoryAdapter's comment on createdAt — matters
        // here specifically because create()+an image does a second save()
        // call on an already-persisted PaymentMethod (see PaymentMethodUseCase#attachImage).
        entity.setCreatedAt(paymentMethod.getCreatedAt());
        PaymentMethodEntity saved = repository.save(entity);

        // imageMimeType is only ever set (by the usecase) on the specific save
        // call that follows an actual upload — a plain field-only save leaves
        // it null, so this only fires when there's really a new image.
        if (paymentMethod.getImageMimeType() != null) {
            attachImage(saved.getPaymentMethodId(), paymentMethod);
        }

        return toDomain(saved);
    }

    @Override
    public void deleteById(Long paymentMethodId) {
        imageRepository.findByPaymentMethodId(paymentMethodId).ifPresent(image -> {
            imageRepository.delete(image);
            attachFileRepository.deleteById(image.getAttachFileId());
        });
        repository.deleteById(paymentMethodId);
    }

    private void attachImage(Long paymentMethodId, PaymentMethod paymentMethod) {
        AttachFileEntity attachFile = new AttachFileEntity();
        attachFile.setName(paymentMethod.getImageObjectKey());
        attachFile.setUrl(paymentMethod.getImageObjectKey());
        attachFile.setMimeType(paymentMethod.getImageMimeType());
        attachFile.setSize(paymentMethod.getImageSize());
        attachFile = attachFileRepository.save(attachFile);

        PaymentMethodImageEntity image = imageRepository.findByPaymentMethodId(paymentMethodId)
                .orElseGet(() -> {
                    PaymentMethodImageEntity created = new PaymentMethodImageEntity();
                    created.setPaymentMethodId(paymentMethodId);
                    return created;
                });
        image.setAttachFileId(attachFile.getAttachFilesId());
        imageRepository.save(image);
    }

    private PaymentMethod toDomain(PaymentMethodEntity entity) {
        String objectKey = imageRepository.findByPaymentMethodId(entity.getPaymentMethodId())
                .flatMap(image -> attachFileRepository.findById(image.getAttachFileId()))
                .map(AttachFileEntity::getUrl)
                .orElse(null);

        return PaymentMethod.builder()
                .paymentMethodId(entity.getPaymentMethodId())
                .methodName(entity.getMethodName())
                .active(entity.isActive())
                .imageObjectKey(objectKey)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
