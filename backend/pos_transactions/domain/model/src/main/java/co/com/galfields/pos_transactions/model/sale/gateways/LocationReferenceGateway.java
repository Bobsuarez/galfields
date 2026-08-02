package co.com.galfields.pos_transactions.model.sale.gateways;

import java.util.Optional;

public interface LocationReferenceGateway {
    Optional<Long> findIdByName(String name);
}
