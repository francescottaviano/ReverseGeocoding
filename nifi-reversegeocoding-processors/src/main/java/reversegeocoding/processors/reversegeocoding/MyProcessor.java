/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reversegeocoding.processors.reversegeocoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PendingResult;
import com.google.maps.TimeZoneApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import static com.google.maps.TimeZoneApi.getTimeZone;

@Tags({"jasmine", "geocode", "country"})
@CapabilityDescription("Reverse Geocoding")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class MyProcessor extends AbstractProcessor {

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

        InputStream inputStream = session.read(flowFile);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            City city = objectMapper.readValue(inputStream, City.class);
            LatLng coordinates = new LatLng(Double.parseDouble(city.getLat()), Double.parseDouble(city.getLon()));
            try {
                GeocodingResult[] geocodingResults;
                geocodingResults = GeocodingApi.reverseGeocode(geoCont, coordinates).await();

                String country = geocodingResults[0].addressComponents[6].longName;
                TimeZone timeZone = TimeZoneApi.getTimeZone(geoCont, coordinates).await();

                // Set country and time zone to the city
                city.setCountry(country);
                city.setTimeZone(timeZone);

                FlowFile output = session.create();
                OutputStream outputStream = session.write(output);
                objectMapper.writeValue(outputStream, city);
                exitWithSuccess(output, session);

            } catch (ApiException | InterruptedException e) {
                exitWithFailure(flowFile, session);
            }
        } catch (IOException e) {
            exitWithFailure(flowFile, session);
        }
    }

    private void exitWithFailure(FlowFile flowFile, ProcessSession session) {
        exit(flowFile, session, FAILURE_RELATIONSHIP);
    }

    private void exitWithSuccess(FlowFile flowFile, ProcessSession session) {
        exit(flowFile, session, SUCCESS_RELATIONSHIP);
    }

    private void exit(FlowFile flowFile, ProcessSession session, Relationship relationship) {
        session.transfer(flowFile, relationship);
    }
}
