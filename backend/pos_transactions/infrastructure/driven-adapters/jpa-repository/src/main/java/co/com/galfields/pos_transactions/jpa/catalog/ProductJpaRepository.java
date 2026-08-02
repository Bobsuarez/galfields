package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
    Page<ProductEntity> findByActiveTrue(Pageable pageable);
}
