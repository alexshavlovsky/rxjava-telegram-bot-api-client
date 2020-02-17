package telegrambot.pollingclient;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;

public interface PollingClient extends AutoCloseable {
    Single<User> getMe();

    Single<Message> sendMessage(String textMessage, Chat chat);

    Single<Update[]> getUpdates(String query);

    Observable<Message> pollMessages();

    Observable<Message> connect(Observable<String> messages, Observable<Chat> chat, Consumer<String> chatNotSetHandler);
}
