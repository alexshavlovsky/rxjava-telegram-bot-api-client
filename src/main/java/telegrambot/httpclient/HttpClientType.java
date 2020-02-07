package telegrambot.httpclient;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum HttpClientType {
    NETTY_ASYNC_HTTP_CLIENT("ahc", "Netty AsyncHttpClient"),
    APACHE_HTTP_ASYNC_CLIENT("apache", "Netflix ApacheHttpClient"),
    SPRING_REACTOR_WEB_CLIENT("spring", "Spring ProjectReactor Netty WebClient");

    private String key;
    private String description;
    public static HttpClientType defaultClient = HttpClientType.NETTY_ASYNC_HTTP_CLIENT;

    HttpClientType(String key, String description) {
        this.key = key;
        this.description = description;
    }

    static public HttpClientType getByKey(String clientKey) {
        for (HttpClientType val : HttpClientType.values()) if (val.key.equals(clientKey)) return val;
        return null;
    }

    static public String joinToString() {
        return Arrays.stream(HttpClientType.values()).map(HttpClientType::toString).collect(Collectors.joining("\n"));
    }

    @Override
    public String toString() {
        return String.format("<%s> - %s", key, description);
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }
}
