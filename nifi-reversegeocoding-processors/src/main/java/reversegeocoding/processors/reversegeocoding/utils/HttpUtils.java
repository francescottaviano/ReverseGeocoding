package reversegeocoding.processors.reversegeocoding.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import java.io.IOException;


public class HttpUtils {

    private HttpUtils() {
    }

    public static HttpResp get(String url) throws IOException {
        HttpResponse response = Request.Get(url).execute().returnResponse();
        return new HttpResp(response.getStatusLine().getStatusCode(), response.getEntity());
    }
}
