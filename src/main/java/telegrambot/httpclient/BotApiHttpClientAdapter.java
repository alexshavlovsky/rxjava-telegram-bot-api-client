package telegrambot.httpclient;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Single;
import telegrambot.apimodel.ApiResponse;

import java.io.IOException;

abstract class BotApiHttpClientAdapter extends BotApiHttpClient implements RawBodyHttpRequests {

    private static <T> Single<T> rawBodyToApiType(byte[] rawBody, Class<T> clazz) {
        ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, clazz);
        try {
            ApiResponse<T> apiResponse = objectMapper.readValue(rawBody, type);
            if (apiResponse.getOk()) return Single.just(apiResponse.getResult());
            else return Single.error(() -> new ApiException(apiResponse));
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    static String apiUri(String token, String method) {
        return String.format("%s/bot%s/%s", API_BASE_URL, token, method);
    }

    static String apiUri(String token, String method, String query) {
        return (query == null || query.trim().isEmpty()) ?
                apiUri(token, method) :
                apiUri(token, method + "?" + query);
    }

    @Override
    final public <T> Single<T> genericGetRequest(String token, String method, String query, Class<T> clazz) {
        return rawBodyGetRequest(token, method, query)
                .flatMap(rawBody -> BotApiHttpClientAdapter.rawBodyToApiType(rawBody, clazz));
    }

    @Override
    final public <T> Single<T> genericPostRequest(String token, String method, String json, Class<T> clazz) {
        return rawBodyPostRequest(token, method, json)
                .flatMap(rawBody -> BotApiHttpClientAdapter.rawBodyToApiType(rawBody, clazz));
    }

}
