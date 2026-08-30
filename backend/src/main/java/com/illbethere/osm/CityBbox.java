package com.illbethere.osm;

import com.illbethere.domain.City;

public record CityBbox(City city, double south, double west, double north, double east) {

    public boolean contains(double lat, double lng) {
        return lat >= south && lat <= north && lng >= west && lng <= east;
    }

    public static final CityBbox HAIFA = new CityBbox(City.HAIFA, 32.7700, 34.9600, 32.8500, 35.0800);
    public static final CityBbox TEL_AVIV = new CityBbox(City.TEL_AVIV, 32.0330, 34.7420, 32.1470, 34.8550);
    public static final CityBbox RAMAT_GAN = new CityBbox(City.RAMAT_GAN, 32.0480, 34.8000, 32.1000, 34.8450);

    public static final CityBbox[] IMPORT_ORDER = {HAIFA, TEL_AVIV, RAMAT_GAN};

    public static City resolveCity(double lat, double lng) {
        if (RAMAT_GAN.contains(lat, lng)) {
            return City.RAMAT_GAN;
        }
        if (TEL_AVIV.contains(lat, lng)) {
            return City.TEL_AVIV;
        }
        if (HAIFA.contains(lat, lng)) {
            return City.HAIFA;
        }
        return City.OTHER;
    }
}
