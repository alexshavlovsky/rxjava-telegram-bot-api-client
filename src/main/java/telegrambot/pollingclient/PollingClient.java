package telegrambot.pollingclient;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;

public interface PollingClient extends AutoCloseable {
    Single<User> getMe();

    Completable sendMessage(Chat chat, String textMessage);

    Single<Update[]> getUpdates(String query);

    Observable<Message> pollMessages();
}
