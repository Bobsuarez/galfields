package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.PaymentMethodRequest;
import co.com.galfields.pos.dto.PaymentMethodResponse;
import co.com.galfields.pos.service.PaymentMethodService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/**
 * A payment method is created/updated as a multipart call, same shape as
 * products: "paymentMethod" (JSON) plus an optional "image" file part.
 */
@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse create(
            @RequestPart("paymentMethod") @Valid PaymentMethodRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return paymentMethodService.createPaymentMethod(request, image);
    }

    @GetMapping
    public List<PaymentMethodResponse> list() {
        return paymentMethodService.listPaymentMethods();
    }

    @GetMapping("/{paymentMethodId}")
    public PaymentMethodResponse get(@PathVariable Long paymentMethodId) {
        return paymentMethodService.getPaymentMethod(paymentMethodId);
    }

    @PutMapping(value = "/{paymentMethodId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PaymentMethodResponse update(
            @PathVariable Long paymentMethodId,
            @RequestPart("paymentMethod") @Valid PaymentMethodRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return paymentMethodService.updatePaymentMethod(paymentMethodId, request, image);
    }

    @DeleteMapping("/{paymentMethodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long paymentMethodId) {
        paymentMethodService.deletePaymentMethod(paymentMethodId);
    }
}
