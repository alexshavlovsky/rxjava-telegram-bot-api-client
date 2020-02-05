package telegrambot.httpclient;

import io.reactivex.Single;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.adapter.rxjava.RxJava2Adapter;

final class SpringWebClient extends AbstractHttpClientApiAdapter {

    private final WebClient httpClient;

    SpringWebClient() {
        httpClient = WebClient.builder().baseUrl("https://api.telegram.org").build();
    }

    private WebClient.RequestBodySpec createRequest(HttpMethod httpMethod, String token, String method, String query) {
        return httpClient
                .method(httpMethod)
                .uri(uriBuilder -> uriBuilder
                        .path("/bot{token}/{method}")
                        .query(query)
                        .build(token, method))
                .accept(MediaType.APPLICATION_JSON);
    }

    @Override
    Single<byte[]> getRequest(String token, String method, String query) {
        return RxJava2Adapter.monoToSingle(
                createRequest(HttpMethod.GET, token, method, query)
                        .retrieve()
                        .bodyToMono(byte[].class)
        );
    }

    @Override
    Single<byte[]> postRequest(String token, String method, String json) {
        return RxJava2Adapter.monoToSingle(
                createRequest(HttpMethod.POST, token, method, "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(json)
                        .retrieve()
                        .bodyToMono(byte[].class)
        );
    }

    @Override
    public void close() {
        // this implementation does nothing
    }
}
