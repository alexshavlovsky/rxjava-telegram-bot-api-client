package telegrambot.httpclient;

import io.reactivex.Single;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;

public abstract class HttpClient implements AutoCloseable, TelegramApi {
    static String API_BASE_URL = "https://api.telegram.org";

    public abstract <T> Single<T> apiGetRequest(String token, String method, String query, Class<T> clazz);

    public abstract <T> Single<T> apiPostRequest(String token, String method, String json, Class<T> clazz);

    @Override
    public Single<User> getMe(String token) {
        return apiGetRequest(token, "getMe", "", User.class);
    }

    @Override
    public Single<Message> sendMessage(String token, Chat chat, String textMessage) {
        String json = String.format("{\"chat_id\":%d,\"text\":\"%s\"}", chat.getId(), textMessage);
        return apiPostRequest(token, "sendMessage", json, Message.class);
    }

    @Override
    public Single<Update[]> getUpdates(String token, String query) {
        return apiGetRequest(token, "getUpdates", query, Update[].class);
    }
}
