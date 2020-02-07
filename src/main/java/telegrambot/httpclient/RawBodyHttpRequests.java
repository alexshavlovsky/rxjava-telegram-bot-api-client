package telegrambot.httpclient;

import io.reactivex.Single;

interface RawBodyHttpRequests {
    Single<byte[]> rawBodyGetRequest(String token, String method, String query);

    Single<byte[]> rawBodyPostRequest(String token, String method, String json);
}
