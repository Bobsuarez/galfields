package co.com.galfields.pos_transactions.model.sale.gateways;

import co.com.galfields.pos_transactions.model.sale.Sale;

import java.util.Optional;

public interface SaleRepository {
    Optional<Sale> findByClientEventId(String clientEventId);

    Optional<Sale> findById(Long transactionId);

    Sale save(Sale sale);
}
