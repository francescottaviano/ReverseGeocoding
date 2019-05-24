package reversegeocoding.processors.reversegeocoding.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import reversegeocoding.processors.reversegeocoding.City;

import java.io.IOException;
import java.io.OutputStream;

/**
 * City Serializer class
 * */

public class CitySerializer implements Serializer<City> {
    private ObjectMapper objectMapper;

    public CitySerializer(){
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void serialize(City city, OutputStream outputStream) throws SerializationException, IOException {
        objectMapper.writeValue(outputStream, city);
    }
}
