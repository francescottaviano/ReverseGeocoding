package reversegeocoding.processors.reversegeocoding;

import com.google.maps.GeocodingApi;
import com.google.maps.TimeZoneApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import reversegeocoding.processors.reversegeocoding.deserializers.CityDeserializer;
import reversegeocoding.processors.reversegeocoding.providers.GeoCodingProvider;
import reversegeocoding.processors.reversegeocoding.providers.GeoCodingProviderFactory;
import reversegeocoding.processors.reversegeocoding.providers.GoogleAPIProvider;
import reversegeocoding.processors.reversegeocoding.serializers.CitySerializer;
import reversegeocoding.processors.reversegeocoding.serializers.StringSerializer;

import java.io.IOException;
import java.util.*;

/**
 * Reverse Geocoding NiFi Processor main class
 * It requires GeoNames username or Google API key as Property
 */

@Tags({"jasmine", "reverse", "geocode", "country", "geonames", "timezone"})
@CapabilityDescription("Reverse Geocoding Processor")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class ReverseGeocodingProcessor extends AbstractProcessor {

    /**
    * Google API Key property is required to use Google services
    */
    public static final PropertyDescriptor GOOGLE_API_KEY_PROP = new PropertyDescriptor
            .Builder().name("GOOGLE_API_KEY_PROP")
            .displayName("Google API key")
            .description("Google API key required to access Google Places services")
            .allowableValues("GOOGLE_API_PROVIDER", "GEO_NAMES_PROVIDER")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    /**
     * GoeoNames username property is required to use GeoNames services
     */
    public static final PropertyDescriptor GEO_NAMES_USERNAME_PROP = new PropertyDescriptor
            .Builder().name("GEO_NAMES_USERNAME_PROP")
            .displayName("GeoNames username")
            .description("GeoNames username required to access GeoNames services")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();


    public static final PropertyDescriptor GEO_CODING_PROVIDER_PROP = new PropertyDescriptor
            .Builder().name("GEO_CODING_PROVIDER_PROP")
            .displayName("GeoCoding Provider")
            .description("GeoCoding Provider")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    /**
     * CSV delimiter string property
     */
    public static final PropertyDescriptor CSV_DELIMETER = new PropertyDescriptor
            .Builder().name("CSV_DELIMITER")
            .displayName("CSV delimiter")
            .description("Set csv delimiter string")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    /**
     * CSV header check
     */
    public static final PropertyDescriptor HAS_HEADER = new PropertyDescriptor
            .Builder().name("HAS_HEADER")
            .displayName("has header")
            .description("Set true if CSV file has header")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("true")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();

    // Identifies the distributed map cache client
    public static final PropertyDescriptor DISTRIBUTED_CACHE_SERVICE = new PropertyDescriptor.Builder()
            .name("Distributed Cache Service")
            .description("The Controller Service that is used to cache flow files")
            .required(true)
            .identifiesControllerService(DistributedMapCacheClient.class)
            .build();

    /**
     * Success Relationship
     */
    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("success")
            .description("Success Relationship")
            .build();

    /**
     * Failure Relationship
     */
    public static final Relationship FAILURE_RELATIONSHIP = new Relationship.Builder()
            .name("failure")
            .description("Failure Relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    /*
     * Processor specific attributes
     */
    private String apiKey;
    private String geoNamesUsername;
    private String csvDelimiter;
    private boolean hasHeader;
    private CacheProvider cacheProvider;
    private GeoCodingProvider geoCodingProvider;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        /*
        * Adding properties to descriptors list
        * */
        descriptors.add(GEO_CODING_PROVIDER_PROP);
        descriptors.add(CSV_DELIMETER);
        descriptors.add(HAS_HEADER);
        descriptors.add(DISTRIBUTED_CACHE_SERVICE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        /*
         * Adding relationships to relationships set
         * */
        relationships.add(SUCCESS_RELATIONSHIP);
        relationships.add(FAILURE_RELATIONSHIP);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        /*
         * Reverse Geocoding service to provide
         * country and timezone information from city coordinates.
         */
        buildGeocodingProvider(context);

        // Get component properties
        csvDelimiter = context.getProperty(CSV_DELIMETER).getValue();
        hasHeader = context.getProperty(HAS_HEADER).asBoolean();

        // Build cache provider
        DistributedMapCacheClient cache = context.getProperty(DISTRIBUTED_CACHE_SERVICE)
                .asControllerService(DistributedMapCacheClient.class);

        this.cacheProvider = new CacheProvider.CacheProviderBuilder()
                .setCache(cache)
                .build();

    }

    private void buildGeocodingProvider(ProcessContext context) {
        GeoCodingProviderFactory.ProviderType providerType = GeoCodingProviderFactory.ProviderType
                .valueOf(context.getProperty(GEO_CODING_PROVIDER_PROP).getValue());


        switch (providerType) {
            case GOOGLE_API_PROVIDER:
                apiKey = context.getProperty(GOOGLE_API_KEY_PROP).getValue();
                if (apiKey == null) {
                    return;
                }
                geoCodingProvider = GeoCodingProviderFactory.createProvider(providerType, apiKey);
                break;
            case GEO_NAMES_PROVIDER:
                geoNamesUsername = context.getProperty(GEO_NAMES_USERNAME_PROP).getValue();
                if (geoNamesUsername == null) {
                    return;
                }
                geoCodingProvider = GeoCodingProviderFactory.createProvider(providerType, geoNamesUsername);
                break;
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        FlowFile output = session.write(flowFile, (in, out) -> {

            CSVReader csvReader = new CSVReader(in, csvDelimiter, hasHeader);
            CSVWriter csvWriter = new CSVWriter(out, csvDelimiter);

            // Header check
            if (!hasHeader) {
                exitWithFailure(flowFile, session);
            }

            // Get header fields
            final List<String> headerFields = csvReader.getHeaderFields();

            // Header size check
            if (headerFields.size() == 0) {
                exitWithFailure(flowFile, session);
            }

            headerFields.add("country");
            headerFields.add("timeOffset");

            //keep track of elements in csv
            HashMap<String, Integer> headerMap = parseCSVHeader(headerFields);

            List<String> line = null;
            csvWriter.writeLine(headerFields);

            //create serializers
            StringSerializer stringSerializer = new StringSerializer();
            CitySerializer citySerializer = new CitySerializer();

            //create deserializers
            CityDeserializer cityDeserializer = new CityDeserializer();

            try {
                while ((line = csvReader.getNextLineFields()) != null) {

                    City city = readCity(line, headerMap);
                    city = reverseGeoCoding(city, cacheProvider, geoCodingProvider, stringSerializer, citySerializer, cityDeserializer);
                    csvWriter.writeLine(Arrays.asList(city.getName(), city.getLat().toString(), city.getLon().toString(), city.getCountry(), city.getTimeOffset()));

                }

                csvReader.closeFile();
                csvWriter.closeFile();
            } catch (ApiException | InterruptedException e) {
                e.printStackTrace();
                csvReader.closeFile();
                csvWriter.closeFile();
                exitWithFailure(flowFile, session);
            }
        });

        exitWithSuccess(output, session);
    }

    /**
     * map csv header
     * @param headerFields
     * @return
     */
    private HashMap<String, Integer> parseCSVHeader(List<String> headerFields) {
        HashMap<String, Integer> map = new HashMap<>();

        for (int i = 0; i < headerFields.size(); i++) {
            map.put(headerFields.get(i), i);
        }

        return map;
    }

    /**
     * read city from file
     * @param line
     * @return
     */
    private City readCity(List<String> line, HashMap<String, Integer> headerMap) {
        return new City(
                line.get(headerMap.get("City")),
                Double.parseDouble(line.get(headerMap.get("Latitude"))),
                Double.parseDouble(line.get(headerMap.get("Longitude")))
        );
    }

    /**
     * reverse geocoding main routine
     * @param city
     * @return
     */
    private City reverseGeoCoding(City city, CacheProvider cacheProvider, GeoCodingProvider geoCodingProvider,
                                  StringSerializer stringSerializer, CitySerializer citySerializer,
                                  CityDeserializer cityDeserializer) throws IOException, ApiException, InterruptedException {
        City cachedCity = readFromCache(city.getName(), cacheProvider, stringSerializer, cityDeserializer);
        if (cachedCity != null) {
            return cachedCity;
        } else {
            City c = askProvider(city, geoCodingProvider);
            putInCache(c, cacheProvider, stringSerializer, citySerializer);
            return c;
        }
    }

    /**
     * read from redis cache
     * @param key
     * @param cacheProvider
     * @param stringSerializer
     * @param cityDeserializer
     * @return
     */
    private City readFromCache(String key, CacheProvider cacheProvider, StringSerializer stringSerializer,
                               CityDeserializer cityDeserializer) throws IOException {
        return cacheProvider.getCache().get(key, stringSerializer, cityDeserializer);
    }

    /**
     * put in cache
     * @param city
     * @param cacheProvider
     * @param stringSerializer
     * @param citySerializer
     */
    private void putInCache(City city, CacheProvider cacheProvider, StringSerializer stringSerializer,
                            CitySerializer citySerializer) throws IOException {
        cacheProvider.getCache().put(city.getName(), city, stringSerializer, citySerializer);
    }

    /**
     * ask reverse geocoding provider
     * @param city
     * @param geoCodingProvider
     * @return
     */
    private City askProvider(City city, GeoCodingProvider geoCodingProvider) throws InterruptedException, ApiException, IOException {
        return geoCodingProvider.resolve(city);
    }

    private void exitWithFailure(FlowFile flowFile, ProcessSession session) throws IOException {
        exit(flowFile, session, FAILURE_RELATIONSHIP);
    }

    private void exitWithSuccess(FlowFile flowFile, ProcessSession session) {
        exit(flowFile, session, SUCCESS_RELATIONSHIP);
    }

    private void exit(FlowFile flowFile, ProcessSession session, Relationship relationship) {
        session.transfer(flowFile, relationship);
    }
}
