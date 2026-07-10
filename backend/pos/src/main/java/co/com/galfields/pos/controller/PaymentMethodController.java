package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.PaymentMethodRequest;
import co.com.galfields.pos.dto.PaymentMethodResponse;
import co.com.galfields.pos.service.PaymentMethodService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse create(@RequestBody @Valid PaymentMethodRequest request) {
        return paymentMethodService.createPaymentMethod(request);
    }

    @GetMapping
    public List<PaymentMethodResponse> list() {
        return paymentMethodService.listPaymentMethods();
    }

    @GetMapping("/{paymentMethodId}")
    public PaymentMethodResponse get(@PathVariable Long paymentMethodId) {
        return paymentMethodService.getPaymentMethod(paymentMethodId);
    }

    @PutMapping("/{paymentMethodId}")
    public PaymentMethodResponse update(
            @PathVariable Long paymentMethodId, @RequestBody @Valid PaymentMethodRequest request) {
        return paymentMethodService.updatePaymentMethod(paymentMethodId, request);
    }

    @DeleteMapping("/{paymentMethodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long paymentMethodId) {
        paymentMethodService.deletePaymentMethod(paymentMethodId);
    }
}
