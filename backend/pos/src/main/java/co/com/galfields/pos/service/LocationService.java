package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.LocationRequest;
import co.com.galfields.pos.dto.LocationResponse;
import co.com.galfields.pos.entity.Location;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.LocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        Location location = new Location();
        applyFields(location, request);
        return toResponse(locationRepository.save(location));
    }

    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long locationId) {
        return toResponse(findOrThrow(locationId));
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> listLocations() {
        return locationRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LocationResponse updateLocation(Long locationId, LocationRequest request) {
        Location location = findOrThrow(locationId);
        applyFields(location, request);
        return toResponse(locationRepository.save(location));
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        Location location = findOrThrow(locationId);
        locationRepository.delete(location);
    }

    private Location findOrThrow(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location " + locationId + " not found"));
    }

    private void applyFields(Location location, LocationRequest request) {
        location.setName(request.name());
        location.setAddress(request.address());
        location.setPhone(request.phone());
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getLocationId(),
                location.getName(),
                location.getAddress(),
                location.getPhone(),
                location.getCreatedAt());
    }
}
