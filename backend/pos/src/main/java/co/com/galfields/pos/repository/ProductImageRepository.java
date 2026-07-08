package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.ProductImage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    Optional<ProductImage> findByProduct_ProductId(Long productId);
}
