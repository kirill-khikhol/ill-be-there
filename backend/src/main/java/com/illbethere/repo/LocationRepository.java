package com.illbethere.repo;

import com.illbethere.domain.City;
import com.illbethere.domain.Location;
import com.illbethere.domain.LocationCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByOsmId(String osmId);

    List<Location> findByCategory(LocationCategory category);

    List<Location> findByCityAndCategory(City city, LocationCategory category);
}
