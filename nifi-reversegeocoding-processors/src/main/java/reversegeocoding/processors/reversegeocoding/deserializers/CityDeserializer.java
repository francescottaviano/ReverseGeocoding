package reversegeocoding.processors.reversegeocoding.deserializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import reversegeocoding.processors.reversegeocoding.City;

import java.io.IOException;

/**
 * City Deserializer class
 * */

public class CityDeserializer implements Deserializer<City> {
    private ObjectMapper objectMapper;

    public CityDeserializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public City deserialize(byte[] bytes) throws DeserializationException, IOException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return objectMapper.readValue(bytes, City.class);
    }
}
