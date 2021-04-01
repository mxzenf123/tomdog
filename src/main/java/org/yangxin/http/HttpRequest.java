package org.yangxin.http;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class HttpRequest {

    private HttpMethod method;
    private Map<String, String> headers = new HashMap<>(5);
    private String requestUri;
    private InputStream inputStream;

    public HttpRequest(InputStream inputStream){
        this.inputStream = inputStream;
    }

    public HttpRequest(){
    }

    public HttpRequest buildHttpLine(byte[] tmp, int offset, int length){
        String[] strs = new String(tmp, offset, length).split(" ");
        this.method = HttpMethod.valueOf(strs[0].trim().toUpperCase());
        this.requestUri = strs[1].trim();
        return this;
    }

    public void addHeader(byte[] tmp, int offset, int length){
        String[] strs = new String(tmp, offset, length).split(":");
        headers.put(strs[0].trim(), strs[1].trim());
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
}
