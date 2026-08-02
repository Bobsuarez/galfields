package co.com.galfields.pos_transactions.jpa.sale.shadow;

import co.com.galfields.pos_transactions.model.sale.gateways.EmployeeReferenceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SaleEmployeeReferenceGatewayAdapter implements EmployeeReferenceGateway {

    private final EmployeeShadowJpaRepository repository;

    @Override
    public Optional<Long> findIdByUsername(String username) {
        return repository.findByUsername(username).map(EmployeeShadowEntity::getEmployeeId);
    }
}
