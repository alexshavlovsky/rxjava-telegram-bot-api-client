package telegrambot.httpclient;

public class HttpClientFactory {
    public static HttpClient newInstance(HttpClientType httpClientType) {
        return httpClientType == HttpClientType.APACHE_HTTP_ASYNC_CLIENT ?
                new ApacheHttpAsyncClient() :
                new SpringWebClient();
    }
}
