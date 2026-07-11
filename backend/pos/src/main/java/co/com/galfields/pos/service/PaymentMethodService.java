package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.PaymentMethodRequest;
import co.com.galfields.pos.dto.PaymentMethodResponse;
import co.com.galfields.pos.entity.AttachFile;
import co.com.galfields.pos.entity.PaymentMethod;
import co.com.galfields.pos.entity.PaymentMethodImage;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.AttachFileRepository;
import co.com.galfields.pos.repository.PaymentMethodRepository;
import co.com.galfields.pos.util.ImageCompressor;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final AttachFileRepository attachFileRepository;
    private final MinioService minioService;
    private final ImageCompressor imageCompressor;

    @Transactional
    public PaymentMethodResponse createPaymentMethod(PaymentMethodRequest request, MultipartFile image) {
        PaymentMethod paymentMethod = new PaymentMethod();
        applyFields(paymentMethod, request);
        paymentMethod = paymentMethodRepository.save(paymentMethod);

        if (image != null && !image.isEmpty()) {
            attachImage(paymentMethod, image);
        }
        return toResponse(paymentMethod);
    }

    @Transactional(readOnly = true)
    public PaymentMethodResponse getPaymentMethod(Long paymentMethodId) {
        return toResponse(findOrThrow(paymentMethodId));
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> listPaymentMethods() {
        return paymentMethodRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentMethodResponse updatePaymentMethod(Long paymentMethodId, PaymentMethodRequest request, MultipartFile image) {
        PaymentMethod paymentMethod = findOrThrow(paymentMethodId);
        applyFields(paymentMethod, request);

        if (image != null && !image.isEmpty()) {
            attachImage(paymentMethod, image);
        }
        return toResponse(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    public void deletePaymentMethod(Long paymentMethodId) {
        PaymentMethod paymentMethod = findOrThrow(paymentMethodId);

        AttachFile attachFile = Optional.ofNullable(paymentMethod.getImage())
                .map(PaymentMethodImage::getAttachFile)
                .orElse(null);

        paymentMethodRepository.delete(paymentMethod);

        if (attachFile != null) {
            minioService.deleteObject(attachFile.getUrl());
            attachFileRepository.delete(attachFile);
        }
    }

    private void attachImage(PaymentMethod paymentMethod, MultipartFile file) {
        byte[] compressed = imageCompressor.compress(file);
        String objectKey = minioService.uploadPaymentMethodImage(paymentMethod, file, compressed);
        AttachFile attachFile = saveAttachFile(file, compressed, objectKey);

        PaymentMethodImage paymentMethodImage = paymentMethod.getImage();
        String previousObjectKey = null;
        if (paymentMethodImage == null) {
            paymentMethodImage = new PaymentMethodImage();
            paymentMethodImage.setPaymentMethod(paymentMethod);
            paymentMethod.setImage(paymentMethodImage);
        } else {
            previousObjectKey = paymentMethodImage.getAttachFile()
                    .getUrl();
        }
        paymentMethodImage.setAttachFile(attachFile);

        if (previousObjectKey != null) {
            minioService.deleteObject(previousObjectKey);
        }
    }

    private AttachFile saveAttachFile(MultipartFile file, byte[] data, String objectKey) {
        AttachFile attachFile = new AttachFile();
        attachFile.setName(file.getOriginalFilename() != null ? file.getOriginalFilename() : objectKey);
        attachFile.setUrl(objectKey);
        attachFile.setMimeType(file.getContentType());
        attachFile.setSize(data.length);
        return attachFileRepository.save(attachFile);
    }

    private PaymentMethod findOrThrow(Long paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod " + paymentMethodId + " not found"));
    }

    private void applyFields(PaymentMethod paymentMethod, PaymentMethodRequest request) {
        paymentMethod.setMethodName(request.methodName());
        paymentMethod.setActive(request.active());
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        String imageUrl = Optional.ofNullable(paymentMethod.getImage())
                .map(PaymentMethodImage::getAttachFile)
                .map(AttachFile::getUrl)
                .map(minioService::getPublicUrl)
                .orElse(null);

        return new PaymentMethodResponse(
                paymentMethod.getPaymentMethodId(),
                paymentMethod.getMethodName(),
                paymentMethod.isActive(),
                imageUrl,
                paymentMethod.getCreatedAt());
    }
}
