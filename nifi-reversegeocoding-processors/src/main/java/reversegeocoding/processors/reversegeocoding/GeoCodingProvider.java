package reversegeocoding.processors.reversegeocoding;

import com.google.maps.GeoApiContext;

public class GeoCodingProvider {

    private GeoApiContext geoApiContext;

    private GeoCodingProvider() {
    }

    public GeoApiContext getGeoApiContext() {
        return geoApiContext;
    }

    public static class GeoCodingProviderBuilder {
        private GeoCodingProvider geoCodingProvider;

        public GeoCodingProviderBuilder() {
            this.geoCodingProvider = new GeoCodingProvider();
        }

        public GeoCodingProviderBuilder setApiKey(String apiKey) {
            this.geoCodingProvider.geoApiContext =  new GeoApiContext.Builder()
                    .apiKey(apiKey)
                    .build();
            return this;
        }

        public GeoCodingProvider build() {
            return this.geoCodingProvider;
        }
    }
}
