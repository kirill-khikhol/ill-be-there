package com.illbethere.service;

import com.illbethere.domain.AppUser;
import com.illbethere.domain.City;
import com.illbethere.domain.Location;
import com.illbethere.domain.LocationCategory;
import com.illbethere.domain.LocationSource;
import com.illbethere.osm.OsmImportService;
import com.illbethere.repo.LocationRepository;
import com.illbethere.web.dto.LocationDtos.CreateLocationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final OsmImportService osmImportService;

    public LocationService(LocationRepository locationRepository, OsmImportService osmImportService) {
        this.locationRepository = locationRepository;
        this.osmImportService = osmImportService;
    }

    public List<Location> list(City city, LocationCategory category) {
        osmImportService.importAllIfEmpty();
        LocationCategory effective = category != null ? category : LocationCategory.SPORTS_GROUND;
        if (city == null) {
            return locationRepository.findByCategory(effective);
        }
        return locationRepository.findByCityAndCategory(city, effective);
    }

    public Location get(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("location_not_found"));
    }

    @Transactional
    public Location create(CreateLocationRequest request, AppUser user) {
        Location location = new Location();
        location.setName(request.name().trim());
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setCategory(request.category() != null ? request.category() : LocationCategory.SPORTS_GROUND);
        location.setSource(LocationSource.USER);
        location.setCity(osmImportService.resolveCity(request.latitude(), request.longitude()));
        location.setCreatedByUserId(user.getId());
        return locationRepository.save(location);
    }
}
