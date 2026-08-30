package com.illbethere.web.dto;

import com.illbethere.domain.City;
import com.illbethere.domain.Location;
import com.illbethere.domain.LocationCategory;
import com.illbethere.domain.LocationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class LocationDtos {

    private LocationDtos() {
    }

    public record LocationResponse(
            Long id,
            String name,
            double latitude,
            double longitude,
            LocationCategory category,
            LocationSource source,
            City city
    ) {
        public static LocationResponse from(Location location) {
            return new LocationResponse(
                    location.getId(),
                    location.getName(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getCategory(),
                    location.getSource(),
                    location.getCity());
        }
    }

    public record CreateLocationRequest(
            @NotBlank String name,
            @NotNull Double latitude,
            @NotNull Double longitude,
            LocationCategory category
    ) {
    }
}
