package co.com.galfields.pos_transactions.model.sale.gateways;

import java.util.Optional;

public interface EmployeeReferenceGateway {
    Optional<Long> findIdByUsername(String username);
}
