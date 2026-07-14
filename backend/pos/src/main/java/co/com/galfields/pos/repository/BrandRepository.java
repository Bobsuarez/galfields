package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findAllByOrderByNameAsc();
}
