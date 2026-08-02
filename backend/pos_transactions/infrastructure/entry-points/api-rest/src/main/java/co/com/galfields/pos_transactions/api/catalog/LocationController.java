package co.com.galfields.pos_transactions.api.catalog;

import co.com.galfields.pos_transactions.model.catalog.Location;
import co.com.galfields.pos_transactions.usecase.catalog.LocationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Mirrors backend/pos's LocationController 1:1. */
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationUseCase locationUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public LocationResponse create(@RequestBody @Valid LocationRequest request) {
        return toResponse(locationUseCase.create(toDomain(request)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<LocationResponse> list() {
        return locationUseCase.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{locationId}")
    @Transactional(readOnly = true)
    public LocationResponse get(@PathVariable("locationId") Long locationId) {
        return toResponse(locationUseCase.get(locationId));
    }

    @PutMapping("/{locationId}")
    @Transactional
    public LocationResponse update(@PathVariable("locationId") Long locationId, @RequestBody @Valid LocationRequest request) {
        return toResponse(locationUseCase.update(locationId, toDomain(request)));
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("locationId") Long locationId) {
        locationUseCase.delete(locationId);
    }

    private Location toDomain(LocationRequest request) {
        return Location.builder().name(request.name()).address(request.address()).phone(request.phone()).build();
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(location.getLocationId(), location.getName(), location.getAddress(), location.getPhone(), location.getCreatedAt());
    }
}
