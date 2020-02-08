package telegrambot.pollingclient;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;
import telegrambot.httpclient.BotApiHttpClient;

import java.util.Arrays;

public class ShortPollingClient implements PollingClient {

    private final String token;
    private final BotApiHttpClient httpClient;
    final PublishSubject<Message> outgoingMessages = PublishSubject.create();

    public ShortPollingClient(String token, BotApiHttpClient httpClient) {
        this.token = token;
        this.httpClient = httpClient;
    }

    @Override
    final public Single<User> getMe() {
        return httpClient.getMe(token);
    }

    @Override
    final public Completable sendMessage(Chat chat, String textMessage) {
        return httpClient.sendMessage(token, chat, textMessage).doOnSuccess(outgoingMessages::onNext).ignoreElement();
    }

    @Override
    final public Single<Update[]> getUpdates(String query) {
        return httpClient.getUpdates(token, query);
    }

    @Override
    public Observable<Message> pollMessages() {
        return getUpdates("")
                .flattenAsObservable(Arrays::asList)
                .map(Update::getMessage)
                .onErrorResumeNext(Observable.never())
                .mergeWith(outgoingMessages);
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }
}
