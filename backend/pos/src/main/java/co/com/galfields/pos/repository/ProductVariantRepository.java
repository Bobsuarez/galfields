package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.ProductVariant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    boolean existsBySku(String sku);

    boolean existsByBarcodeAndProduct_ProductIdNot(String barcode, Long productId);

    boolean existsBySkuAndProduct_ProductIdNot(String sku, Long productId);
}
