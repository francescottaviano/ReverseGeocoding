package reversegeocoding.processors.reversegeocoding.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import reversegeocoding.processors.reversegeocoding.City;
import reversegeocoding.processors.reversegeocoding.utils.GeoNamesEntity;
import reversegeocoding.processors.reversegeocoding.utils.HttpResp;
import reversegeocoding.processors.reversegeocoding.utils.HttpUtils;

import java.io.IOException;

/**
 * GeoNames Provider class
 * */

public class GeoNamesProvider implements GeoCodingProvider {
    private String baseUrl;
    private String username;

    public GeoNamesProvider() {
        this.baseUrl = "http://api.geonames.org/timezoneJSON";
    }

    @Override
    public City resolve(City city) {

        try {
            String url = buildGeoNamesUrl(this.baseUrl, city.getLat().toString(), city.getLon().toString(), this.username);
            HttpResp response = HttpUtils.get(url);
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                ObjectMapper objectMapper = new ObjectMapper();
                GeoNamesEntity entity = objectMapper.readValue(response.getContent().getContent(), GeoNamesEntity.class);
                city.setCountry(entity.getCountryName());
                city.setTimezone((entity.getTimezoneId()));
                return city;
            } else {
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    private String buildGeoNamesUrl(String baseUrl, String lat, String lon, String username) {
        return String.format("%s?lat=%s&lng=%s&username=%s", baseUrl, lat, lon, username);
    }

    public static class GeoNamesProviderBuilder {
        private GeoNamesProvider geoNamesProvider;

        public GeoNamesProviderBuilder() {
            this.geoNamesProvider = new GeoNamesProvider();
        }

        public GeoNamesProviderBuilder setUsername(String username) {
            this.geoNamesProvider.username = username;
            return this;
        }

        public GeoNamesProvider build() {
            return this.geoNamesProvider;
        }
    }
}
