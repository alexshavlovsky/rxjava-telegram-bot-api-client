package telegrambot.httpclient;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Single;
import telegrambot.apimodel.ApiResponse;

import java.io.IOException;

abstract class AbstractHttpClientApiAdapter implements HttpClient {

    private static <T> Single<T> fromByteArray(byte[] rawResponse, Class<T> clazz) {
        ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, clazz);
        try {
            ApiResponse<T> apiResponse = objectMapper.readValue(rawResponse, type);
            if (apiResponse.getOk()) return Single.just(apiResponse.getResult());
            return Single.error(() -> new ApiException(apiResponse));
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    abstract Single<byte[]> getRequest(String token, String method, String query);

    abstract Single<byte[]> postRequest(String token, String method, String json);

    @Override
    final public <T> Single<T> apiGetRequest(String token, String method, String query, Class<T> clazz) {
        return getRequest(token, method, query)
                .flatMap(byteArray -> AbstractHttpClientApiAdapter.fromByteArray(byteArray, clazz));
    }

    @Override
    final public <T> Single<T> apiPostRequest(String token, String method, String json, Class<T> clazz) {
        return postRequest(token, method, json)
                .flatMap(byteArray -> AbstractHttpClientApiAdapter.fromByteArray(byteArray, clazz));
    }
}
