package telegrambot.httpclient;

public class HttpClientFactory {
    public static HttpClient newInstance(HttpClientType httpClientType) {
        switch (httpClientType) {
            case SPRING_REACTOR_WEB_CLIENT:
                return new SpringWebClientEmbeddedMapper();
            case APACHE_HTTP_ASYNC_CLIENT:
                return new ApacheHttpAsyncClient();
            case ASYNC_HTTP_CLIENT:
            default:
                return new OrgAsyncHttpClient();
        }
    }
}
