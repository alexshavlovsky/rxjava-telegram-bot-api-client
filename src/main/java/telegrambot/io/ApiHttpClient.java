package telegrambot.io;

import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.apache.http.ObservableHttp;
import rx.apache.http.ObservableHttpResponse;
import telegrambot.apimodel.ApiResponse;
import telegrambot.apimodel.User;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ApiHttpClient implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ApiHttpClient.class);
    private final CloseableHttpAsyncClient httpClient;

    public ApiHttpClient() {
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
    }

    private static String apiUri(String token, String method) {
        return String.format("https://api.telegram.org/bot%s/%s", token, method);
    }

    public Observable<ObservableHttpResponse> apiGetRequest(String token, String method) {
        var uri = apiUri(token, method);
        var producer = HttpAsyncMethods.createGet(uri);
        return ObservableHttp.createRequest(producer, httpClient).toObservable();
    }

    public Observable<ObservableHttpResponse> apiPostRequest(String token, String method, String json) {
        try {
            var uri = apiUri(token, method);
            var producer = HttpAsyncMethods.createPost(uri, json, ContentType.APPLICATION_JSON);
            return ObservableHttp.createRequest(producer, httpClient).toObservable();
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
    }

    private static <T extends ApiResponse> T catchAndPropagateApiError(T response) {
        if (!response.getOk()) throw new RuntimeException(response.toString());
        return response;
    }

    public static <T> Observable<T> parseHttpResponse(Observable<ObservableHttpResponse> response, Class<T> clazz) {
        return response
                .flatMap(ObservableHttpResponse::getContent)
                .flatMap(content -> ApiResponse.fromByteArray(content, clazz))
                .map(ApiHttpClient::catchAndPropagateApiError)
                .map(ApiResponse::getResult);
    }

    public User validateTokenAgainstApiBlocking(String token) {
        return parseHttpResponse(apiGetRequest(token, "getMe"), User.class)
                .doOnError(e -> logger.error("Validate token against API: {}", e.getMessage()))
                .onErrorReturn((e) -> null)
                .toBlocking().single();
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
