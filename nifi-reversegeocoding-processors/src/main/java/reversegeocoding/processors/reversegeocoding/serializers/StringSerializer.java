package reversegeocoding.processors.reversegeocoding.serializers;

import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StringSerializer implements Serializer<String> {

    @Override
    public void serialize(final String value, final OutputStream out) throws SerializationException, IOException {
        out.write(value.getBytes(StandardCharsets.UTF_8));
    }
}
