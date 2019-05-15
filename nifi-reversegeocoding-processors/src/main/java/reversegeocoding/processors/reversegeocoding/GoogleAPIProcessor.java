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
import reversegeocoding.processors.reversegeocoding.serializers.CitySerializer;
import reversegeocoding.processors.reversegeocoding.serializers.StringSerializer;

import java.io.IOException;
import java.util.*;

/**
 * Google Places API NiFi Processor main class
 * It requires API Key as Property
 */

@Tags({"jasmine", "reverse", "geocode", "country", "google", "places", "timezone"})
@CapabilityDescription("Reverse Geocoding")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class GoogleAPIProcessor extends AbstractProcessor {

    /*
    * Google API Key property is required to use Google services
    * */
    public static final PropertyDescriptor GOOGLE_API_KEY_PROP = new PropertyDescriptor
            .Builder().name("GOOGLE_API_KEY_PROP")
            .displayName("Google API key")
            .description("Google API key to access Google Places services")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    /*
     * CSV delimiter string
     * */
    public static final PropertyDescriptor CSV_DELIMETER = new PropertyDescriptor
            .Builder().name("CSV_DELIMITER")
            .displayName("csv delimiter")
            .description("set csv delimiter string")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    /*
     * CSV header check
     * */
    public static final PropertyDescriptor HAS_HEADER = new PropertyDescriptor
            .Builder().name("HAS_HEADER")
            .displayName("has header")
            .description("Set true if csv file has header")
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

    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("success")
            .description("Success Relationship")
            .build();

    public static final Relationship FAILURE_RELATIONSHIP = new Relationship.Builder()
            .name("failure")
            .description("Failure Relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    /**
     * processor specific attributes
     * */
    // Get component properties
    private String apiKey;
    private String csvDelimiter;
    private boolean hasHeader;
    private CacheProvider cacheProvider;
    private GeoCodingProvider geoCodingProvider;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(GOOGLE_API_KEY_PROP);
        descriptors.add(CSV_DELIMETER);
        descriptors.add(HAS_HEADER);
        descriptors.add(DISTRIBUTED_CACHE_SERVICE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
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
         * Reverse Geocoding Google service to provide
         * country and timezone information from city coordinates.
         */
        // Build GeoApiContext with API Key provided by Property value
        apiKey = context.getProperty(GOOGLE_API_KEY_PROP).getValue();
        geoCodingProvider = new GeoCodingProvider.GeoCodingProviderBuilder()
                .setApiKey(apiKey)
                .build();

        // Get component properties
        csvDelimiter = context.getProperty(CSV_DELIMETER).getValue();
        hasHeader = context.getProperty(HAS_HEADER).asBoolean();

        // build cache provider
        DistributedMapCacheClient cache = context.getProperty(DISTRIBUTED_CACHE_SERVICE)
                .asControllerService(DistributedMapCacheClient.class);

        this.cacheProvider = new CacheProvider.CacheProviderBuilder()
                .setCache(cache)
                .build();

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
                    city = reverseGeoCoding(city, cacheProvider, stringSerializer, citySerializer, cityDeserializer);
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
    private City reverseGeoCoding(City city, CacheProvider cacheProvider,
                                  StringSerializer stringSerializer, CitySerializer citySerializer,
                                  CityDeserializer cityDeserializer) throws IOException, ApiException, InterruptedException {
        City cachedCity = readFromCache(city.getName(), cacheProvider, stringSerializer, cityDeserializer);
        if (cachedCity != null) {
            return cachedCity;
        } else {
            /*City c = askProvider(city);
            putInCache(c, cacheProvider, stringSerializer, citySerializer);
            return c;*/
            City c = new City("root", 0.0, 0.0);
            c.setCountry("root");
            c.parseTimeOffset(-2.43);
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
     * @return
     */
    private City askProvider(City city) throws InterruptedException, ApiException, IOException {
        LatLng coordinates = new LatLng(city.getLat(), city.getLon());
        GeocodingResult[] geoCodingResults = GeocodingApi.reverseGeocode(geoCodingProvider.getGeoApiContext(), coordinates).await();

        for (int i = 0; i < geoCodingResults[0].addressComponents.length; i++) {
            if (geoCodingResults[0].addressComponents[i].types[0] == AddressComponentType.COUNTRY) {
                city.setCountry(geoCodingResults[0].addressComponents[i].longName);
                break;
            }
        }
        // Set time zone to the city
        double offset = TimeZoneApi.getTimeZone(geoCodingProvider.getGeoApiContext(), coordinates).await().getRawOffset()/3600000.0;
        city.parseTimeOffset(offset);
        return city;
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
