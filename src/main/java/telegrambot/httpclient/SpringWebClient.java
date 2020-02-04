package telegrambot.httpclient;

import io.reactivex.Single;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Mono;
import telegrambot.apimodel.ApiResponse;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;

import javax.management.modelmbean.InvalidTargetObjectTypeException;
import java.util.HashMap;
import java.util.Map;

class SpringWebClient implements HttpClient {

    private static final Map<Class, ParameterizedTypeReference> TYPE_REFERENCES = new HashMap<>(2);

    static {
        TYPE_REFERENCES.put(User.class, new ParameterizedTypeReference<ApiResponse<User>>() {
        });
        TYPE_REFERENCES.put(Message.class, new ParameterizedTypeReference<ApiResponse<Message>>() {
        });
        TYPE_REFERENCES.put(Update[].class, new ParameterizedTypeReference<ApiResponse<Update[]>>() {
        });
    }

    private <T> ParameterizedTypeReference<ApiResponse<T>> getTypeRef(Class<T> clazz) throws InvalidTargetObjectTypeException {
        if (!TYPE_REFERENCES.containsKey(clazz))
            throw new InvalidTargetObjectTypeException(clazz.toString());
        return TYPE_REFERENCES.get(clazz);
    }

    private final WebClient httpClient;

    SpringWebClient() {
        httpClient = WebClient.builder().baseUrl("https://api.telegram.org").build();
    }

    private WebClient.RequestBodySpec prepareRequestBody(HttpMethod httpMethod, String token, String method, String query) {
        return httpClient
                .method(httpMethod)
                .uri(uriBuilder -> uriBuilder
                        .path("/bot{token}/{method}")
                        .query(query)
                        .build(token, method))
                .accept(MediaType.APPLICATION_JSON);
    }

    private <T> Mono<T> apiGetRequestMono(String token, String method, String query, Class<T> clazz) {
        try {
            return prepareRequestBody(HttpMethod.GET, token, method, query)
                    .retrieve()
                    .bodyToMono(getTypeRef(clazz))
                    .flatMap(SpringWebClient::catchAndPropagateApiError);
        } catch (InvalidTargetObjectTypeException e) {
            return Mono.error(e);
        }
    }

    private <T> Mono<T> apiPostRequestMono(String token, String method, String json, Class<T> clazz) {
        try {
            return prepareRequestBody(HttpMethod.POST, token, method, "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(json)
                    .retrieve()
                    .bodyToMono(getTypeRef(clazz))
                    .flatMap(SpringWebClient::catchAndPropagateApiError);
        } catch (InvalidTargetObjectTypeException e) {
            return Mono.error(e);
        }
    }

    private static <T> Mono<T> catchAndPropagateApiError(ApiResponse<T> response) {
        if (!response.getOk()) return Mono.error(new RuntimeException(response.getErrorDescription()));
        return Mono.just(response.getResult());
    }

    @Override
    public <T> Single<T> apiGetRequest(String token, String method, String query, Class<T> clazz) {
        return RxJava2Adapter.monoToSingle(apiGetRequestMono(token, method, query, clazz));
    }

    @Override
    public <T> Single<T> apiPostRequest(String token, String method, String json, Class<T> clazz) {
        return RxJava2Adapter.monoToSingle(apiPostRequestMono(token, method, json, clazz));
    }

    @Override
    public void close() {
        // this implementation do nothing
    }
}
