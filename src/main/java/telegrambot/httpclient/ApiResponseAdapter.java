package telegrambot.httpclient;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Single;
import telegrambot.apimodel.ApiResponse;

import java.io.IOException;

class ApiResponseAdapter {

    private static <T> Single<T> mapRawResponseToApiResponse(byte[] rawResponse, Class<T> clazz) {
        ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, clazz);
        try {
            ApiResponse<T> apiResponse = objectMapper.readValue(rawResponse, type);
            if (apiResponse.getOk()) return Single.just(apiResponse.getResult());
            return Single.error(() -> new IOException(apiResponse.getErrorDescription()));
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    static <T> Single<T> fromByteArray(Single<byte[]> rawResponse, Class<T> clazz) {
        return rawResponse.flatMap(raw -> ApiResponseAdapter.mapRawResponseToApiResponse(raw, clazz));
    }
}
