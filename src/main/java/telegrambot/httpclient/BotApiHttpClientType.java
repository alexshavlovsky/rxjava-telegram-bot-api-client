package telegrambot.httpclient;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum BotApiHttpClientType {
    NETTY_ASYNC_HTTP_CLIENT("ahc", "Netty AsyncHttpClient"),
    APACHE_HTTP_ASYNC_CLIENT("apache", "Netflix ApacheHttpClient"),
    SPRING_REACTOR_WEB_CLIENT("spring", "Spring ProjectReactor Netty WebClient");

    private String key;
    private String description;
    public static BotApiHttpClientType defaultClient = BotApiHttpClientType.NETTY_ASYNC_HTTP_CLIENT;

    BotApiHttpClientType(String key, String description) {
        this.key = key;
        this.description = description;
    }

    static public BotApiHttpClientType getByKey(String clientKey) {
        for (BotApiHttpClientType val : BotApiHttpClientType.values()) if (val.key.equals(clientKey)) return val;
        return null;
    }

    static public String joinToString() {
        return Arrays.stream(BotApiHttpClientType.values()).map(BotApiHttpClientType::toString).collect(Collectors.joining("\n"));
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
