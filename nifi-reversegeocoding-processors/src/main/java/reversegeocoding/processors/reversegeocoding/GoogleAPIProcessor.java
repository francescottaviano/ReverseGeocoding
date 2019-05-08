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
import java.util.*;

/**
 * Google Places API NiFi Processor main class
 * It requires API Key as Property
 */

@Tags({"jasmine", "geocode", "country"})
@CapabilityDescription("Reverse Geocoding")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class GoogleAPIProcessor extends AbstractProcessor {

    public static final PropertyDescriptor GOOGLE_API_KEY_PROP = new PropertyDescriptor
            .Builder().name("GOOGLE_API_KEY_PROP")
            .displayName("Google API key")
            .description("Google API key to access Google Places services")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
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
         * country information from city name and coordinates.
         */

        // Build GeoApiContext with API Key provided by Property value
        GeoApiContext geoCont = new GeoApiContext.Builder()
                .apiKey(context.getProperty(GOOGLE_API_KEY_PROP).getValue())
                .build();

        FlowFile output = session.write(flowFile, (in, out) -> {
            ObjectMapper om = new ObjectMapper();

            try {

                City city = om.readValue(in, City.class);
                LatLng coordinates = new LatLng(Double.parseDouble(city.getLat()), Double.parseDouble(city.getLon()));

                GeocodingResult[] geocodingResults;
                geocodingResults = GeocodingApi.reverseGeocode(geoCont, coordinates).await();
                String country = "";

                for (int i = 0; i < geocodingResults[0].addressComponents.length; i++){
                    if (geocodingResults[0].addressComponents[i].types[0] == AddressComponentType.COUNTRY){
                        country = geocodingResults[0].addressComponents[i].longName;
                        city.setCountry(country);
                        break;
                    }
                }

                TimeZone timeZone = TimeZoneApi.getTimeZone(geoCont, coordinates).await();

                // Set time zone to the city
                city.setTimeZone(timeZone);

                om.writeValue(out, city);



            } catch (ApiException | InterruptedException e) {
                e.printStackTrace();
                out.close();
                exitWithFailure(flowFile, session);
            } finally {
                in.close();
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
