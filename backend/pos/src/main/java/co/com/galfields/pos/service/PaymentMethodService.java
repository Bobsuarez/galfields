package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.PaymentMethodRequest;
import co.com.galfields.pos.dto.PaymentMethodResponse;
import co.com.galfields.pos.entity.PaymentMethod;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.PaymentMethodRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    public PaymentMethodResponse createPaymentMethod(PaymentMethodRequest request) {
        PaymentMethod paymentMethod = new PaymentMethod();
        applyFields(paymentMethod, request);
        return toResponse(paymentMethodRepository.save(paymentMethod));
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
    public PaymentMethodResponse updatePaymentMethod(Long paymentMethodId, PaymentMethodRequest request) {
        PaymentMethod paymentMethod = findOrThrow(paymentMethodId);
        applyFields(paymentMethod, request);
        return toResponse(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    public void deletePaymentMethod(Long paymentMethodId) {
        PaymentMethod paymentMethod = findOrThrow(paymentMethodId);
        paymentMethodRepository.delete(paymentMethod);
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
        return new PaymentMethodResponse(
                paymentMethod.getPaymentMethodId(),
                paymentMethod.getMethodName(),
                paymentMethod.isActive(),
                paymentMethod.getCreatedAt());
    }
}
