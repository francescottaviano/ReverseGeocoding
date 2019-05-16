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
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class GoogleAPIProvider extends GeoCodingProviderImpl {

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
            double offset = TimeZoneApi.getTimeZone(geoApiContext, coordinates).await().getRawOffset()/3600000.0;
            city.setTimeOffset(prettyOffset(offset));
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
