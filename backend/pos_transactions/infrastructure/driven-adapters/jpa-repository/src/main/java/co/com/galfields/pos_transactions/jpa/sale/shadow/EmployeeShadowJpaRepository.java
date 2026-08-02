package co.com.galfields.pos_transactions.jpa.sale.shadow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeShadowJpaRepository extends JpaRepository<EmployeeShadowEntity, Long> {
    Optional<EmployeeShadowEntity> findByUsername(String username);
}
