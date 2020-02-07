package telegrambot.httpclient;

import io.reactivex.Single;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;

public abstract class BotApiHttpClient implements AutoCloseable, BotApiRequests, GenericHttpRequests {

    static String API_BASE_URL = "https://api.telegram.org";

    @Override
    final public Single<User> getMe(String token) {
        return genericGetRequest(token, "getMe", "", User.class);
    }

    @Override
    final public Single<Message> sendMessage(String token, Chat chat, String textMessage) {
        String json = String.format("{\"chat_id\":%d,\"text\":\"%s\"}", chat.getId(), textMessage);
        return genericPostRequest(token, "sendMessage", json, Message.class);
    }

    @Override
    final public Single<Update[]> getUpdates(String token, String query) {
        return genericGetRequest(token, "getUpdates", query, Update[].class);
    }

}
