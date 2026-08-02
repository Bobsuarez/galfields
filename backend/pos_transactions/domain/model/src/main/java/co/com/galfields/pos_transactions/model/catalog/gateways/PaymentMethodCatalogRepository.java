package co.com.galfields.pos_transactions.model.catalog.gateways;

import co.com.galfields.pos_transactions.model.catalog.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodCatalogRepository {
    Optional<PaymentMethod> findById(Long paymentMethodId);

    List<PaymentMethod> findAllOrderByMethodName();

    PaymentMethod save(PaymentMethod paymentMethod);

    void deleteById(Long paymentMethodId);
}
