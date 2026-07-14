package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.PaymentMethod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findAllByOrderByMethodNameAsc();
}
