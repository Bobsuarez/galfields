package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}
