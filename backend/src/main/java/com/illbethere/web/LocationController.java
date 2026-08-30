package com.illbethere.web;

import com.illbethere.domain.City;
import com.illbethere.domain.LocationCategory;
import com.illbethere.service.LocationService;
import com.illbethere.service.PromiseService;
import com.illbethere.web.dto.LocationDtos.CreateLocationRequest;
import com.illbethere.web.dto.LocationDtos.LocationResponse;
import com.illbethere.web.dto.PromiseDtos.DaySlotsResponse;
import com.illbethere.web.dto.PromiseDtos.SlotDetailsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;
    private final PromiseService promiseService;

    public LocationController(LocationService locationService, PromiseService promiseService) {
        this.locationService = locationService;
        this.promiseService = promiseService;
    }

    @GetMapping
    public List<LocationResponse> list(
            @RequestParam(required = false) City city,
            @RequestParam(required = false) LocationCategory category) {
        return locationService.list(city, category).stream().map(LocationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public LocationResponse get(@PathVariable Long id) {
        return LocationResponse.from(locationService.get(id));
    }

    @PostMapping
    public LocationResponse create(@Valid @RequestBody CreateLocationRequest request) {
        return LocationResponse.from(locationService.create(request, CurrentUser.require()));
    }

    @GetMapping("/{id}/promises")
    public DaySlotsResponse promises(@PathVariable Long id, @RequestParam String date) {
        return promiseService.daySlots(id, date, CurrentUser.optional());
    }

    @GetMapping("/{id}/promises/details")
    public SlotDetailsResponse details(
            @PathVariable Long id,
            @RequestParam String date,
            @RequestParam String slot) {
        return promiseService.slotDetails(id, date, slot);
    }
}
