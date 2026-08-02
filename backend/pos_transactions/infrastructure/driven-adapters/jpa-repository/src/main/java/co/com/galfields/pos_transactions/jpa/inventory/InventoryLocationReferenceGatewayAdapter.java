package co.com.galfields.pos_transactions.jpa.inventory;

import co.com.galfields.pos_transactions.model.inventory.gateways.LocationReferenceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryLocationReferenceGatewayAdapter implements LocationReferenceGateway {

    private final LocationRefJpaRepository repository;

    @Override
    public Optional<Long> findIdByName(String name) {
        return repository.findByName(name).map(LocationRefEntity::getLocationId);
    }
}
