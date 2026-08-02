package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {
    List<BrandEntity> findAllByOrderByNameAsc();
}
