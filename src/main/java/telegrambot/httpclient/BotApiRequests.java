package telegrambot.httpclient;

import io.reactivex.Single;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;

interface BotApiRequests {
    Single<User> getMe(String token);

    Single<Message> sendMessage(String token, Chat chat, String textMessage);

    Single<Update[]> getUpdates(String token, String query);
}
