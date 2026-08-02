package co.com.galfields.pos_transactions.model.sale.gateways;

public interface PaymentMethodReferenceGateway {
    boolean existsById(Long paymentMethodId);
}
