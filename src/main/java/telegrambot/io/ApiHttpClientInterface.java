package telegrambot.io;

import io.reactivex.Single;

public interface ApiHttpClientInterface extends AutoCloseable {
    <T> Single<T> apiGetRequest(String token, String method, String query, Class<T> clazz);

    <T> Single<T> apiPostRequest(String token, String method, String json, Class<T> clazz);
}
