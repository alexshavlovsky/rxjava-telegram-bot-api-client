package telegrambot.httpclient;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Single;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import rx.apache.http.ObservableHttp;
import rx.apache.http.ObservableHttpResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

final class ApacheHttpAsyncClient extends BotApiHttpClientAdapter {
    private final CloseableHttpAsyncClient httpClient;

    ApacheHttpAsyncClient() {
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
    }

    private Single<byte[]> createRequest(HttpAsyncRequestProducer producer) {
        return RxJavaInterop.toV2Observable(
                ObservableHttp
                        .createRequest(producer, httpClient)
                        .toObservable()
                        .flatMap(ObservableHttpResponse::getContent)
        ).singleOrError();
    }

    @Override
    public Single<byte[]> rawBodyGetRequest(String token, String method, String query) {
        String uri = apiUri(token, method, query);
        HttpAsyncRequestProducer producer = HttpAsyncMethods.createGet(uri);
        return createRequest(producer);
    }

    @Override
    public Single<byte[]> rawBodyPostRequest(String token, String method, String json) {
        try {
            String uri = apiUri(token, method);
            HttpAsyncRequestProducer producer = HttpAsyncMethods.createPost(uri, json, ContentType.APPLICATION_JSON);
            return createRequest(producer);
        } catch (UnsupportedEncodingException e) {
            return Single.error(e);
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
