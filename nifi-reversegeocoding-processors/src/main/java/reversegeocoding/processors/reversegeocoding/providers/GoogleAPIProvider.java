package reversegeocoding.processors.reversegeocoding.providers;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.TimeZoneApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import reversegeocoding.processors.reversegeocoding.City;

import java.io.IOException;

/**
 * Google API Provider class
 * */

public class GoogleAPIProvider implements GeoCodingProvider {

    private GeoApiContext geoApiContext;

    private GoogleAPIProvider() {
    }

    @Override
    public City resolve(City city) {
        try {
            LatLng coordinates = new LatLng(city.getLat(), city.getLon());
            GeocodingResult[] geoCodingResults = GeocodingApi.reverseGeocode(geoApiContext, coordinates).await();

            for (int i = 0; i < geoCodingResults[0].addressComponents.length; i++) {
                if (geoCodingResults[0].addressComponents[i].types[0] == AddressComponentType.COUNTRY) {
                    city.setCountry(geoCodingResults[0].addressComponents[i].longName);
                    break;
                }
            }
            // Set time zone to the city
            String timezone = TimeZoneApi.getTimeZone(geoApiContext, coordinates).await().getID();
            city.setTimezone(timezone);
            return city;

        } catch (ApiException | InterruptedException | IOException e) {
            return null;
        }
    }

    public static class GeoCodingProviderBuilder {
        private GoogleAPIProvider googleAPIProvider;

        public GeoCodingProviderBuilder() {
            this.googleAPIProvider = new GoogleAPIProvider();
        }

        public GeoCodingProviderBuilder setApiKey(String apiKey) {
            this.googleAPIProvider.geoApiContext =  new GeoApiContext.Builder()
                    .apiKey(apiKey)
                    .build();
            return this;
        }

        public GoogleAPIProvider build() {
            return this.googleAPIProvider;
        }
    }
}
