package co.com.galfields.pos_transactions.api.catalog;

import co.com.galfields.pos_transactions.model.PageQuery;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.catalog.Product;
import co.com.galfields.pos_transactions.model.catalog.ProductVariant;
import co.com.galfields.pos_transactions.model.catalog.VariantAttribute;
import co.com.galfields.pos_transactions.usecase.catalog.ProductUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mirrors backend/pos's ProductController 1:1 — same multipart shape
 * ("product" JSON + optional "image" + "variants" JSON array + one
 * "variantImage_&lt;index&gt;" file part per variant with an image), same
 * sortable-property whitelist (per-variant fields like price/sku/stock
 * aren't sortable this way — they live on ProductVariant, one level down).
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private static final Pattern VARIANT_IMAGE_PART_NAME = Pattern.compile("^variantImage_(\\d+)$");

    private static final Map<String, String> SORTABLE_PROPERTIES = Map.of(
            "productId", "productId",
            "name", "name",
            "active", "active",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "categoryName", "categoryId",
            "brandName", "brandId"
    );

    private final ProductUseCase productUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductResponse create(
            @RequestPart("product") @Valid ProductRequest product,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart("variants") @NotEmpty List<@Valid ProductVariantRequest> variants,
            MultipartHttpServletRequest request
    ) {
        Product created = productUseCase.create(toDomain(product, variants), toUploadedImage(image), extractVariantImages(request));
        return toResponse(created);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        PageResult<Product> page = productUseCase.list(toPageQuery(pageable), includeInactive);
        return new PagedResponse<>(page.content().stream().map(this::toResponse).toList(),
                page.totalElements(), page.totalPages(), page.page(), page.size());
    }

    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public ProductResponse get(@PathVariable("productId") Long productId) {
        return toResponse(productUseCase.get(productId));
    }

    @PutMapping("/{productId}/activate")
    @Transactional
    public ProductResponse activate(@PathVariable("productId") Long productId) {
        return toResponse(productUseCase.activate(productId));
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ProductResponse update(
            @PathVariable("productId") Long productId,
            @RequestPart("product") @Valid ProductRequest product,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "variants", required = false) List<@Valid ProductVariantRequest> variants,
            MultipartHttpServletRequest request
    ) {
        List<ProductVariant> variantPatches = variants == null ? null : variants.stream().map(this::toDomain).toList();
        Product updated = productUseCase.update(productId, toDomain(product), toUploadedImage(image),
                variantPatches, extractVariantImages(request));
        return toResponse(updated);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("productId") Long productId) {
        productUseCase.deactivate(productId);
    }

    private PageQuery toPageQuery(Pageable pageable) {
        List<PageQuery.SortOrder> orders = pageable.getSort().stream()
                .map(order -> {
                    String property = SORTABLE_PROPERTIES.get(order.getProperty());
                    if (property == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_PROPERTIES.keySet());
                    }
                    return new PageQuery.SortOrder(property, order.getDirection().isAscending());
                })
                .toList();
        return new PageQuery(pageable.getPageNumber(), pageable.getPageSize(), orders);
    }

    private Map<Integer, ProductUseCase.UploadedImage> extractVariantImages(MultipartHttpServletRequest request) {
        Map<Integer, ProductUseCase.UploadedImage> variantImages = new HashMap<>();
        request.getFileMap().forEach((partName, file) -> {
            Matcher matcher = VARIANT_IMAGE_PART_NAME.matcher(partName);
            if (matcher.matches() && !file.isEmpty()) {
                variantImages.put(Integer.parseInt(matcher.group(1)), toUploadedImage(file));
            }
        });
        return variantImages;
    }

    private ProductUseCase.UploadedImage toUploadedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return new ProductUseCase.UploadedImage(file.getBytes(), file.getContentType(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded image", e);
        }
    }

    private Product toDomain(ProductRequest request, List<ProductVariantRequest> variants) {
        Product product = toDomain(request);
        product.setVariants(variants.stream().map(this::toDomain).toList());
        return product;
    }

    private Product toDomain(ProductRequest request) {
        return Product.builder()
                .name(request.name())
                .description(request.description())
                .categoryId(request.categoryId())
                .brandId(request.brandId())
                .active(true)
                .variants(List.of())
                .build();
    }

    private ProductVariant toDomain(ProductVariantRequest request) {
        List<VariantAttribute> attributes = request.attributes() == null ? null
                : request.attributes().stream()
                        .map(a -> VariantAttribute.builder().attributeName(a.name()).attributeValue(a.value()).build())
                        .toList();
        return ProductVariant.builder()
                .sku(request.sku())
                .barcode(request.barcode())
                .price(request.price())
                .costPrice(request.costPrice())
                .initialStock(request.initialStock())
                .active(true)
                .attributes(attributes)
                .build();
    }

    private ProductResponse toResponse(Product product) {
        List<ProductVariantResponse> variants = product.getVariants().stream().map(this::toResponse).toList();
        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getCategoryId(),
                product.getCategoryName(),
                product.getBrandId(),
                product.getBrandName(),
                product.getImageUrl(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                variants);
    }

    private ProductVariantResponse toResponse(ProductVariant variant) {
        List<VariantAttributeResponse> attributes = variant.getAttributes() == null ? List.of()
                : variant.getAttributes().stream()
                        .map(a -> new VariantAttributeResponse(a.getAttributeName(), a.getAttributeValue()))
                        .toList();
        List<ProductUnitResponse> units = variant.getUnits() == null ? List.of()
                : variant.getUnits().stream()
                        .map(u -> new ProductUnitResponse(u.getProductUnitId(), u.getUnitName(), u.getConversionFactor(),
                                u.getUnitPrice(), u.getStock(), u.getBarcode(), u.isBase(), u.isActive()))
                        .toList();

        return new ProductVariantResponse(
                variant.getVariantId(),
                variant.getSku(),
                variant.getBarcode(),
                variant.getPrice(),
                variant.getCostPrice(),
                variant.getStock(),
                variant.getImageUrl(),
                variant.isActive(),
                attributes,
                units);
    }

    public record PagedResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    }
}
