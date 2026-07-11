package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.ProductRequest;
import co.com.galfields.pos.dto.ProductResponse;
import co.com.galfields.pos.dto.ProductVariantRequest;
import co.com.galfields.pos.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A product and its variants are created/updated in a single multipart call:
 * "product" (JSON), an optional "image" file for the product, an optional
 * "variants" JSON array (at least one is required on create), and one file
 * part per variant image named "variantImage_&lt;index&gt;", where &lt;index&gt;
 * is the zero-based position of the variant in the "variants" array.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private static final Pattern VARIANT_IMAGE_PART_NAME = Pattern.compile("^variantImage_(\\d+)$");

    // Sort keys clients may request (?sort=<key>,asc|desc) mapped to the actual
    // JPA property path on Product. findByActiveTrue(Pageable) applies the sort
    // straight to the Product query, so only Product-level (and to-one
    // association) columns are sortable this way - per-variant fields like
    // price/sku/stock live on ProductVariant and aren't included.
    private static final Map<String, String> SORTABLE_PROPERTIES = Map.of(
            "productId", "productId",
            "name", "name",
            "active", "active",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "categoryName", "category.name",
            "brandName", "brand.name"
    );

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @RequestPart("product") @Valid ProductRequest product,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart("variants") @NotEmpty List<@Valid ProductVariantRequest> variants,
            MultipartHttpServletRequest request
    ) {
        return productService.createProduct(product, image, variants, extractVariantImages(request));
    }

    @GetMapping
    public PagedModel<ProductResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse> page =  productService.listProducts(remapSort(pageable));
        return new PagedModel<>(page);
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable Long productId) {
        return productService.getProduct(productId);
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse update(
            @PathVariable Long productId,
            @RequestPart("product") @Valid ProductRequest product,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "variants", required = false) List<@Valid ProductVariantRequest> variants,
            MultipartHttpServletRequest request
    ) {
        return productService.updateProduct(productId, product, image, variants, extractVariantImages(request));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long productId) {
        productService.deleteProduct(productId);
    }

    private Pageable remapSort(Pageable pageable) {
        List<Sort.Order> orders = pageable.getSort()
                .stream()
                .map(order -> {
                    String jpaProperty = SORTABLE_PROPERTIES.get(order.getProperty());
                    if (jpaProperty == null) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_PROPERTIES.keySet()
                        );
                    }
                    return new Sort.Order(order.getDirection(), jpaProperty);
                })
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    private Map<Integer, MultipartFile> extractVariantImages(MultipartHttpServletRequest request) {
        Map<Integer, MultipartFile> variantImages = new HashMap<>();
        request.getFileMap()
                .forEach((partName, file) -> {
                    Matcher matcher = VARIANT_IMAGE_PART_NAME.matcher(partName);
                    if (matcher.matches()) {
                        variantImages.put(Integer.parseInt(matcher.group(1)), file);
                    }
                });
        return variantImages;
    }
}
