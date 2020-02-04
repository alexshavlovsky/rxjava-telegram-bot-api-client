package telegrambot.httpclient;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Single;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import rx.Observable;
import rx.apache.http.ObservableHttp;
import rx.apache.http.ObservableHttpResponse;
import telegrambot.apimodel.ApiResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

class ApacheHttpAsyncClient implements HttpClient {
    private final CloseableHttpAsyncClient httpClient;

    ApacheHttpAsyncClient() {
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
    }

    private static String apiUri(String token, String method) {
        return String.format("https://api.telegram.org/bot%s/%s", token, method);
    }

    private Observable<ObservableHttpResponse> apiGetRequest(String token, String method) {
        String uri = apiUri(token, method);
        HttpAsyncRequestProducer producer = HttpAsyncMethods.createGet(uri);
        return ObservableHttp.createRequest(producer, httpClient).toObservable();
    }

    private Observable<ObservableHttpResponse> apiPostRequest(String token, String method, String json) {
        try {
            String uri = apiUri(token, method);
            HttpAsyncRequestProducer producer = HttpAsyncMethods.createPost(uri, json, ContentType.APPLICATION_JSON);
            return ObservableHttp.createRequest(producer, httpClient).toObservable();
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
    }

    private static <T extends ApiResponse> T catchAndPropagateApiError(T response) {
        if (!response.getOk()) throw new RuntimeException(response.getErrorDescription());
        return response;
    }

    private static <T> Observable<T> parseApiHttpResponse(Observable<ObservableHttpResponse> response, Class<T> clazz) {
        return response
                .flatMap(ObservableHttpResponse::getContent)
                .flatMap(content -> ApiResponse.fromByteArrayAsObservable(content, clazz))
                .map(ApacheHttpAsyncClient::catchAndPropagateApiError)
                .map(ApiResponse::getResult);
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    private static <T> Single<T> responseToSingle(Observable<ObservableHttpResponse> response, Class<T> clazz) {
        return RxJavaInterop.toV2Observable(parseApiHttpResponse(response, clazz)).singleOrError();
    }

    @Override
    public <T> Single<T> apiGetRequest(String token, String method, String query, Class<T> clazz) {
        return responseToSingle(apiGetRequest(token, method + "?" + query), clazz);
    }

    @Override
    public <T> Single<T> apiPostRequest(String token, String method, String json, Class<T> clazz) {
        return responseToSingle(apiPostRequest(token, method, json), clazz);
    }
}
