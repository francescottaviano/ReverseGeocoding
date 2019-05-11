package reversegeocoding.processors.reversegeocoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
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
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
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

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(GOOGLE_API_KEY_PROP);
        descriptors.add(CSV_DELIMETER);
        descriptors.add(HAS_HEADER);
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
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        /*
         * Reverse Geocoding Google service to provide
         * country and timezone information from city coordinates.
         */

        // Build GeoApiContext with API Key provided by Property value
        GeoApiContext geoCont = new GeoApiContext.Builder()
                .apiKey(context.getProperty(GOOGLE_API_KEY_PROP).getValue())
                .build();

        // get component properties
        String csvDelimiter = context.getProperty(CSV_DELIMETER).getValue();
        boolean hasHeader = context.getProperty(HAS_HEADER).asBoolean();

        FlowFile output = session.write(flowFile, (in, out) -> {

            CSVReader csvReader = new CSVReader(in, csvDelimiter, hasHeader);
            CSVWriter csvWriter = new CSVWriter(out, csvDelimiter);

            // header check
            if (!hasHeader) {
                exitWithFailure(flowFile, session);
            }

            // get header fields
            final List<String> headerFields = csvReader.getHeaderFields();

            // header size check
            if (headerFields.size() == 0) {
                exitWithFailure(flowFile, session);
            }

            headerFields.add("country");
            headerFields.add("timeZone");

            List<String> lines = null;
            //HashMap<String, City> hashMap= new HashMap<>();
            csvWriter.writeLine(headerFields);

            try {
                while ((lines = csvReader.getNextLineFields()) != null) {
                    LatLng coordinates = new LatLng(Double.parseDouble(lines.get(1)), Double.parseDouble(lines.get(2)));
                    GeocodingResult[] geocodingResults;
                    geocodingResults = GeocodingApi.reverseGeocode(geoCont, coordinates).await();
                    City city = new City(lines.get(0), lines.get(1), lines.get(2));

                    for (int i = 0; i < geocodingResults[0].addressComponents.length; i++) {
                        if (geocodingResults[0].addressComponents[i].types[0] == AddressComponentType.COUNTRY) {
                            lines.add(geocodingResults[0].addressComponents[i].longName);
                            city.setCountry(geocodingResults[0].addressComponents[i].longName);
                            break;
                        }
                    }
                    // Set time zone to the city
                    city.setTimeZone(TimeZoneApi.getTimeZone(geoCont, coordinates).await());

                    csvWriter.writeLine(Arrays.asList(city.getName(), city.getLat(), city.getLon(), city.getCountry(), city.getTimeZone().getDisplayName()));

                    //hashMap.put(city.getName(), city);

                }

                csvReader.closeFile();
                csvWriter.closeFile();

                /*ObjectOutput objectOutput = new ObjectOutputStream(out);
                objectOutput.writeObject(hashMap);
                objectOutput.close();*/
            } catch (ApiException | InterruptedException e) {
                e.printStackTrace();
                csvReader.closeFile();
                csvWriter.closeFile();
                exitWithFailure(flowFile, session);
            }
        });

        exitWithSuccess(output, session);
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
