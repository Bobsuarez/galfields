package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.BrandRequest;
import co.com.galfields.pos.dto.BrandResponse;
import co.com.galfields.pos.service.BrandService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse create(@RequestBody @Valid BrandRequest request) {
        return brandService.createBrand(request);
    }

    @GetMapping
    public List<BrandResponse> list() {
        return brandService.listBrands();
    }

    @GetMapping("/{brandId}")
    public BrandResponse get(@PathVariable Long brandId) {
        return brandService.getBrand(brandId);
    }

    @PutMapping("/{brandId}")
    public BrandResponse update(@PathVariable Long brandId, @RequestBody @Valid BrandRequest request) {
        return brandService.updateBrand(brandId, request);
    }

    @DeleteMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long brandId) {
        brandService.deleteBrand(brandId);
    }
}
