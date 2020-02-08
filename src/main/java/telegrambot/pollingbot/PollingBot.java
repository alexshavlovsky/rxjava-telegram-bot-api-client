package telegrambot.pollingbot;

import io.reactivex.Observable;
import io.reactivex.Single;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;

public interface PollingBot extends AutoCloseable {
    Single<User> getMe();

    Single<Message> sendMessage(Chat chat, String textMessage);

    Single<Update[]> getUpdates(String query);

    Observable<Message> getMessages();
}
