package co.com.galfields.pos_transactions.model.catalog.gateways;

import co.com.galfields.pos_transactions.model.PageQuery;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.catalog.Product;
import co.com.galfields.pos_transactions.model.catalog.ProductVariant;

import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long productId);

    PageResult<Product> findPage(PageQuery query, boolean includeInactive);

    Product save(Product product);

    Optional<ProductVariant> findVariantById(Long variantId);

    boolean existsVariantWithSku(String sku, Long excludeProductId);

    boolean existsVariantWithBarcode(String barcode, Long excludeProductId);
}
