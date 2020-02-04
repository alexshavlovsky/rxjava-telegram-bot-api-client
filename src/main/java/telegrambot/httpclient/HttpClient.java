package telegrambot.httpclient;

import io.reactivex.Single;

public interface HttpClient extends AutoCloseable {
    <T> Single<T> apiGetRequest(String token, String method, String query, Class<T> clazz);

    <T> Single<T> apiPostRequest(String token, String method, String json, Class<T> clazz);
}
