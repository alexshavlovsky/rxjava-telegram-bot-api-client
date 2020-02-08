package telegrambot.pollingbot;

import io.reactivex.Observable;
import io.reactivex.Single;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;
import telegrambot.httpclient.BotApiHttpClient;

import java.util.Arrays;

public class ShortPollingBot implements PollingBot {

    private final String token;
    private final BotApiHttpClient httpClient;

    public ShortPollingBot(String token, BotApiHttpClient httpClient) {
        this.token = token;
        this.httpClient = httpClient;
    }

    @Override
    public Single<User> getMe() {
        return httpClient.getMe(token);
    }

    @Override
    public Single<Message> sendMessage(Chat chat, String textMessage) {
        return httpClient.sendMessage(token, chat, textMessage);
    }

    @Override
    public Single<Update[]> getUpdates(String query) {
        return httpClient.getUpdates(token, query);
    }

    @Override
    public Observable<Message> getMessages() {
        return getUpdates("").flattenAsObservable(Arrays::asList).map(Update::getMessage);
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }

}
