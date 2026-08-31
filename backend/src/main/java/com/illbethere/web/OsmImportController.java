package com.illbethere.web;

import com.illbethere.config.AppProperties;
import com.illbethere.osm.OsmImportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/osm")
public class OsmImportController {

    private final OsmImportService osmImportService;
    private final AppProperties properties;

    public OsmImportController(OsmImportService osmImportService, AppProperties properties) {
        this.osmImportService = osmImportService;
        this.properties = properties;
    }

    @PostMapping("/import")
    public OsmImportService.ImportResult importAll(
            @RequestHeader(value = "X-Import-Token", required = false) String token) {
        if (token == null || !token.equals(properties.getOsmImportToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_import_token");
        }
        return osmImportService.importAll();
    }
}
