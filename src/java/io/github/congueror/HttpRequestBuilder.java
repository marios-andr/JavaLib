package io.github.congueror;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestBuilder {
    private URI uri;
    private final List<String> body = new ArrayList<>();
    private final Map<String, String> headers = new HashMap<>();
    private String requestType = "GET";

    public HttpRequestBuilder(URI uri) {
        this.uri = uri;
    }

    public HttpRequestBuilder GET() {
        this.requestType = "GET";
        return this;
    }

    public HttpRequestBuilder POST() {
        this.requestType = "POST";
        return this;
    }

    public HttpRequestBuilder PUT() {
        this.requestType = "PUT";
        return this;
    }

    public HttpRequestBuilder DELETE() {
        this.requestType = "DELETE";
        return this;
    }

    public HttpRequestBuilder HEAD() {
        this.requestType = "HEAD";
        return this;
    }

    public HttpRequestBuilder header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public HttpRequestBuilder addParameter(String name, int value) {
        body.add(name + "=" + value);
        return this;
    }

    public HttpRequestBuilder addParameter(String name, String value) {
        body.add(name + "=" + value);
        return this;
    }

    private boolean hasBody() {
        return requestType.equals("POST") || requestType.equals("PUT");
    }

    public HttpRequest build() {
        String bodyString = String.join("&", body);

        if (!hasBody()) {
            String a = uri.toString();
            if (uri.getQuery() == null) {
                a = a + "?" + bodyString;
            } else
                a = a + "&" + bodyString;
            try {
                this.uri = new URI(a);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        HttpRequest.Builder request = HttpRequest.newBuilder(uri);

        if (hasBody()) {
            request.method(requestType, bodyString.isEmpty() ?
                    HttpRequest.BodyPublishers.noBody() :
                    HttpRequest.BodyPublishers.ofString(bodyString));
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.header(entry.getKey(), entry.getValue());
        }

        return request.build();
    }
}
