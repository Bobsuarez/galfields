package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.LocationRequest;
import co.com.galfields.pos.dto.LocationResponse;
import co.com.galfields.pos.service.LocationService;
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
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse create(@RequestBody @Valid LocationRequest request) {
        return locationService.createLocation(request);
    }

    @GetMapping
    public List<LocationResponse> list() {
        return locationService.listLocations();
    }

    @GetMapping("/{locationId}")
    public LocationResponse get(@PathVariable Long locationId) {
        return locationService.getLocation(locationId);
    }

    @PutMapping("/{locationId}")
    public LocationResponse update(@PathVariable Long locationId, @RequestBody @Valid LocationRequest request) {
        return locationService.updateLocation(locationId, request);
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long locationId) {
        locationService.deleteLocation(locationId);
    }
}
