package reversegeocoding.processors.reversegeocoding.utils;

import org.apache.http.HttpEntity;

/**
 * HTTP Response class
 * */

public class HttpResp {
    private int status;
    private HttpEntity content;

    public HttpResp(int status, HttpEntity content) {
        this.status = status;
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public HttpEntity getContent() {
        return content;
    }
}
