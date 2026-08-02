package co.com.galfields.pos_transactions.jpa.sale.shadow;

import co.com.galfields.pos_transactions.model.sale.gateways.PaymentMethodReferenceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SalePaymentMethodReferenceGatewayAdapter implements PaymentMethodReferenceGateway {

    private final PaymentMethodShadowJpaRepository repository;

    @Override
    public boolean existsById(Long paymentMethodId) {
        return repository.existsById(paymentMethodId);
    }
}
