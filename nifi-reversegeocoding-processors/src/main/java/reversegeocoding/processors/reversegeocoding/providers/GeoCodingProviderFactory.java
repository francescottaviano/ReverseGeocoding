package reversegeocoding.processors.reversegeocoding.providers;

public class GeoCodingProviderFactory {

    public enum ProviderType {
        GOOGLE_API_PROVIDER,
        GEO_NAMES_PROVIDER
    }

    private GeoCodingProviderFactory() {}

    public static GeoCodingProvider createProvider(ProviderType type, String... info) {
        switch (type) {
            case GEO_NAMES_PROVIDER:
                return createGeoNamesProvider(info);
            case GOOGLE_API_PROVIDER:
                return createGoogleAPIProvider(info);
            default:
                return null;
        }
    }

    private static GeoCodingProvider createGoogleAPIProvider(String[] info) {
        if (info.length == 1) {
            String apiKey = info[0];
            return new GoogleAPIProvider.GeoCodingProviderBuilder().setApiKey(apiKey).build();
        } else {
            return null;
        }
    }

    private static GeoCodingProvider createGeoNamesProvider(String... info) {
        if (info.length == 1) {
            String username = info[0];
            return new GeoNamesProvider.GeoNamesProviderBuilder().setUsername(username).build();
        } else {
            return null;
        }
    }
}
