package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.ProductRequest;
import co.com.galfields.pos.dto.ProductResponse;
import co.com.galfields.pos.entity.Brand;
import co.com.galfields.pos.entity.Category;
import co.com.galfields.pos.entity.Inventory;
import co.com.galfields.pos.entity.Location;
import co.com.galfields.pos.entity.Product;
import co.com.galfields.pos.entity.ProductVariant;
import co.com.galfields.pos.exception.DuplicateResourceException;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.BrandRepository;
import co.com.galfields.pos.repository.CategoryRepository;
import co.com.galfields.pos.repository.InventoryRepository;
import co.com.galfields.pos.repository.LocationRepository;
import co.com.galfields.pos.repository.ProductRepository;
import co.com.galfields.pos.repository.ProductVariantRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String DEFAULT_LOCATION_NAME = "Bodega Principal";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final MinioService minioService;

    @Transactional
    public ProductResponse createProduct(ProductRequest request, MultipartFile image) {
        if (productVariantRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("A product variant with sku '" + request.sku() + "' already exists");
        }
        if (productVariantRepository.existsByBarcode(request.barcode())) {
            throw new DuplicateResourceException("A product with barcode '" + request.barcode() + "' already exists");
        }

        Product product = new Product();
        applyProductFields(product, request);
        product = productRepository.save(product);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        applyVariantFields(variant, request);
        productVariantRepository.save(variant);
        product.getVariants().add(variant);

        Inventory inventory = new Inventory();
        inventory.setVariant(variant);
        inventory.setLocation(defaultLocation());
        inventory.setQuantityOnHand(request.initialStock() != null ? request.initialStock() : 0);
        inventoryRepository.save(inventory);

        if (image != null && !image.isEmpty()) {
            product.setImageObjectKey(minioService.uploadProductImage(product, image));
            product = productRepository.save(product);
        }

        return toResponse(product, variant);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = findProductOrThrow(productId);
        return toResponse(product, primaryVariant(product));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
                .map(product -> toResponse(product, primaryVariant(product)));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, MultipartFile image) {
        Product product = findProductOrThrow(productId);
        ProductVariant variant = primaryVariant(product);

        if (productVariantRepository.existsBySkuAndProduct_ProductIdNot(request.sku(), productId)) {
            throw new DuplicateResourceException("A product variant with sku '" + request.sku() + "' already exists");
        }
        if (productVariantRepository.existsByBarcodeAndProduct_ProductIdNot(request.barcode(), productId)) {
            throw new DuplicateResourceException("A product with barcode '" + request.barcode() + "' already exists");
        }

        applyProductFields(product, request);
        applyVariantFields(variant, request);

        if (image != null && !image.isEmpty()) {
            String previousImageObjectKey = product.getImageObjectKey();
            product.setImageObjectKey(minioService.uploadProductImage(product, image));
            minioService.deleteObject(previousImageObjectKey);
        }

        if (request.initialStock() != null) {
            Location location = defaultLocation();
            Inventory inventory = inventoryRepository
                    .findByVariant_VariantIdAndLocation_LocationId(variant.getVariantId(), location.getLocationId())
                    .orElseGet(() -> {
                        Inventory created = new Inventory();
                        created.setVariant(variant);
                        created.setLocation(location);
                        return created;
                    });
            inventory.setQuantityOnHand(request.initialStock());
            inventoryRepository.save(inventory);
        }

        product = productRepository.save(product);
        productVariantRepository.save(variant);
        return toResponse(product, variant);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProductOrThrow(productId);
        product.setActive(false);
        product.getVariants().forEach(v -> v.setActive(false));
        productRepository.save(product);
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
    }

    private ProductVariant primaryVariant(Product product) {
        return product.getVariants().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product " + product.getProductId() + " has no variant"));
    }

    private Location defaultLocation() {
        return locationRepository.findByName(DEFAULT_LOCATION_NAME)
                .orElseThrow(() -> new ResourceNotFoundException("Default location '" + DEFAULT_LOCATION_NAME + "' not found"));
    }

    private Integer stockOf(ProductVariant variant) {
        Location location = defaultLocation();
        return inventoryRepository
                .findByVariant_VariantIdAndLocation_LocationId(variant.getVariantId(), location.getLocationId())
                .map(Inventory::getQuantityOnHand)
                .orElse(0);
    }

    private void applyProductFields(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category " + request.categoryId() + " not found"));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        if (request.brandId() != null) {
            Brand brand = brandRepository.findById(request.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand " + request.brandId() + " not found"));
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }
    }

    private void applyVariantFields(ProductVariant variant, ProductRequest request) {
        variant.setSku(request.sku());
        variant.setBarcode(request.barcode());
        variant.setPrice(request.price());
        variant.setCostPrice(request.costPrice());
    }

    private ProductResponse toResponse(Product product, ProductVariant variant) {
        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                Optional.ofNullable(product.getCategory()).map(Category::getCategoryId).orElse(null),
                Optional.ofNullable(product.getCategory()).map(Category::getName).orElse(null),
                Optional.ofNullable(product.getBrand()).map(Brand::getBrandId).orElse(null),
                Optional.ofNullable(product.getBrand()).map(Brand::getName).orElse(null),
                variant.getVariantId(),
                variant.getSku(),
                variant.getBarcode(),
                variant.getPrice(),
                variant.getCostPrice(),
                stockOf(variant),
                minioService.getPresignedUrl(product.getImageObjectKey()),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
