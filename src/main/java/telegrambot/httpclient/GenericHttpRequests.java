package telegrambot.httpclient;

import io.reactivex.Single;

interface GenericHttpRequests {
    <T> Single<T> genericGetRequest(String token, String method, String query, Class<T> clazz);

    <T> Single<T> genericPostRequest(String token, String method, String json, Class<T> clazz);
}
