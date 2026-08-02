package co.com.galfields.pos_transactions.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariantEntity, Long> {
    List<ProductVariantEntity> findByProductId(Long productId);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsBySkuAndProductIdNot(String sku, Long productId);

    boolean existsByBarcodeAndProductIdNot(String barcode, Long productId);
}
