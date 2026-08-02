package co.com.galfields.pos_transactions.jpa.sale.shadow;

import co.com.galfields.pos_transactions.model.sale.gateways.LocationReferenceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SaleLocationReferenceGatewayAdapter implements LocationReferenceGateway {

    private final LocationShadowJpaRepository repository;

    @Override
    public Optional<Long> findIdByName(String name) {
        return repository.findByName(name).map(LocationShadowEntity::getLocationId);
    }
}
