package com.illbethere.osm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illbethere.domain.City;
import com.illbethere.domain.Location;
import com.illbethere.domain.LocationCategory;
import com.illbethere.domain.LocationSource;
import com.illbethere.repo.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class OsmImportService {

    private static final Logger log = LoggerFactory.getLogger(OsmImportService.class);
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    private final LocationRepository locationRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean importing = new AtomicBoolean(false);

    public OsmImportService(LocationRepository locationRepository, ObjectMapper objectMapper) {
        this.locationRepository = locationRepository;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(90_000);
        this.restClient = RestClient.builder()
                .baseUrl(OVERPASS_URL)
                .defaultHeader("User-Agent", "ill-be-there/0.1")
                .requestFactory(factory)
                .build();
    }

    public ImportResult importAllIfEmpty() {
        if (locationRepository.count() > 0) {
            return new ImportResult(0, 0, "already-populated");
        }
        return importAll();
    }

    public ImportResult importAll() {
        if (!importing.compareAndSet(false, true)) {
            return new ImportResult(0, 0, "already-running");
        }
        try {
            int created = 0;
            int updated = 0;
            for (CityBbox bbox : CityBbox.IMPORT_ORDER) {
                ImportResult part = importBbox(bbox);
                created += part.created();
                updated += part.updated();
            }
            return new ImportResult(created, updated, "ok");
        } finally {
            importing.set(false);
        }
    }

    @Transactional
    public ImportResult importBbox(CityBbox bbox) {
        String query = """
                [out:json][timeout:60];
                (
                  nwr["leisure"="pitch"](%s,%s,%s,%s);
                  nwr["leisure"="sports_centre"](%s,%s,%s,%s);
                  nwr["leisure"="fitness_station"](%s,%s,%s,%s);
                );
                out center tags;
                """.formatted(
                bbox.south(), bbox.west(), bbox.north(), bbox.east(),
                bbox.south(), bbox.west(), bbox.north(), bbox.east(),
                bbox.south(), bbox.west(), bbox.north(), bbox.east());

        String body = restClient.post()
                .contentType(MediaType.TEXT_PLAIN)
                .body(query)
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            return new ImportResult(0, 0, "empty-response");
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode elements = root.path("elements");
            int created = 0;
            int updated = 0;
            List<Location> batch = new ArrayList<>();
            for (JsonNode element : elements) {
                String type = element.path("type").asText();
                long osmNumericId = element.path("id").asLong();
                if (osmNumericId == 0) {
                    continue;
                }
                String osmId = type + "/" + osmNumericId;
                Double lat = readCoord(element, "lat");
                Double lon = readCoord(element, "lon");
                if (lat == null || lon == null) {
                    lat = readCoord(element.path("center"), "lat");
                    lon = readCoord(element.path("center"), "lon");
                }
                if (lat == null || lon == null) {
                    continue;
                }
                String name = pickName(element.path("tags"));
                Location existing = locationRepository.findByOsmId(osmId).orElse(null);
                if (existing != null) {
                    existing.setName(name);
                    existing.setLatitude(lat);
                    existing.setLongitude(lon);
                    batch.add(existing);
                    updated++;
                } else {
                    Location location = new Location();
                    location.setName(name);
                    location.setLatitude(lat);
                    location.setLongitude(lon);
                    location.setCategory(LocationCategory.SPORTS_GROUND);
                    location.setSource(LocationSource.OSM);
                    location.setCity(bbox.city());
                    location.setOsmId(osmId);
                    batch.add(location);
                    created++;
                }
            }
            locationRepository.saveAll(batch);
            log.info("OSM import {} created={} updated={}", bbox.city(), created, updated);
            return new ImportResult(created, updated, "ok");
        } catch (Exception e) {
            log.error("OSM import failed for {}", bbox.city(), e);
            throw new IllegalStateException("OSM import failed for " + bbox.city(), e);
        }
    }

    public City resolveCity(double lat, double lng) {
        return CityBbox.resolveCity(lat, lng);
    }

    private static Double readCoord(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asDouble();
    }

    private static String pickName(JsonNode tags) {
        for (String key : List.of("name:en", "name:ru", "name", "name:he")) {
            String value = tags.path(key).asText(null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        String sport = tags.path("sport").asText(null);
        if (sport != null && !sport.isBlank()) {
            return sport.replace('_', ' ') + " pitch";
        }
        return "Sports ground";
    }

    public record ImportResult(int created, int updated, String status) {
    }
}
