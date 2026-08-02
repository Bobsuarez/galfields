package co.com.galfields.pos_transactions.api.catalog;

import co.com.galfields.pos_transactions.model.catalog.PaymentMethod;
import co.com.galfields.pos_transactions.usecase.catalog.PaymentMethodUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

/** Mirrors backend/pos's PaymentMethodController — multipart, same shape as products. */
@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodUseCase paymentMethodUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PaymentMethodResponse create(
            @RequestPart("paymentMethod") @Valid PaymentMethodRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return toResponse(paymentMethodUseCase.create(toDomain(request), toUploadedImage(image)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> list() {
        return paymentMethodUseCase.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{paymentMethodId}")
    @Transactional(readOnly = true)
    public PaymentMethodResponse get(@PathVariable("paymentMethodId") Long paymentMethodId) {
        return toResponse(paymentMethodUseCase.get(paymentMethodId));
    }

    @PutMapping(value = "/{paymentMethodId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public PaymentMethodResponse update(
            @PathVariable("paymentMethodId") Long paymentMethodId,
            @RequestPart("paymentMethod") @Valid PaymentMethodRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return toResponse(paymentMethodUseCase.update(paymentMethodId, toDomain(request), toUploadedImage(image)));
    }

    @DeleteMapping("/{paymentMethodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("paymentMethodId") Long paymentMethodId) {
        paymentMethodUseCase.delete(paymentMethodId);
    }

    private PaymentMethod toDomain(PaymentMethodRequest request) {
        return PaymentMethod.builder().methodName(request.methodName()).active(request.active()).build();
    }

    private PaymentMethodUseCase.UploadedImage toUploadedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return new PaymentMethodUseCase.UploadedImage(file.getBytes(), file.getContentType(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded image", e);
        }
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                paymentMethod.getPaymentMethodId(),
                paymentMethod.getMethodName(),
                paymentMethod.isActive(),
                paymentMethod.getImageUrl(),
                paymentMethod.getCreatedAt());
    }
}
