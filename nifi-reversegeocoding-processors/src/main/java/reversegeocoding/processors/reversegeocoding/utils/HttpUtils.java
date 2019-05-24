package reversegeocoding.processors.reversegeocoding.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import java.io.IOException;

/**
 * HTTP Utils class
 * */

public class HttpUtils {

    private HttpUtils() {
    }

    public static HttpResp get(String url) throws IOException {
        HttpResponse response = Request.Get(url).execute().returnResponse();
        return new HttpResp(response.getStatusLine().getStatusCode(), response.getEntity());
    }
}
