package telegrambot.pollingclient;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;
import telegrambot.httpclient.BotApiHttpClient;

import java.util.Arrays;

public class ShortPollingClient implements PollingClient {
    private final String token;
    private final BotApiHttpClient httpClient;
    private final Logger logger;

    public ShortPollingClient(String token, BotApiHttpClient httpClient, Logger logger) {
        this.token = token;
        this.httpClient = httpClient;
        this.logger = logger;
    }

    @Override
    final public Single<User> getMe() {
        return httpClient.getMe(token)
                .doOnError(e -> logger.error("API method 'getMe' processing error: {}", e.toString()))
                .onErrorResumeNext(Single.never());
    }

    @Override
    final public Single<Message> sendMessage(Chat chat, String textMessage) {
        return httpClient.sendMessage(token, chat, textMessage)
                .doOnError(e -> logger.error("API method 'sendMessage' processing error: {}", e.toString()))
                .onErrorResumeNext(Single.never());
    }

    @Override
    final public Single<Update[]> getUpdates(String query) {
        return httpClient.getUpdates(token, query)
                .doOnError(e -> logger.error("API method 'getUpdates' processing error: {}", e.toString()));
    }

    @Override
    public Observable<Message> pollMessages() {
        return getUpdates("")
                .flattenAsObservable(Arrays::asList)
                .map(Update::getMessage)
                .onErrorResumeNext(Observable.never());
    }

    @Override
    public Observable<Message> connect(Observable<String> messages, Observable<Chat> chat, Consumer<String> chatNotSetHandler) {
        Completable notifyChatNotSet = messages.takeUntil(chat).doOnNext(chatNotSetHandler).ignoreElements();
        Observable<String> outgoingMessage = messages.skipUntil(chat).mergeWith(notifyChatNotSet);
        return Observable.combineLatest(chat, outgoingMessage, this::sendMessage).flatMapSingle(c -> c)
                .mergeWith(pollMessages().takeUntil(messages.lastElement().toObservable()));
    }

    @Override
    final public void close() throws Exception {
        httpClient.close();
    }
}
