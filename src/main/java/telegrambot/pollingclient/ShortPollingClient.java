package telegrambot.pollingclient;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
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
    final PublishSubject<Message> outgoingMessages$ = PublishSubject.create();
    private Disposable messageToChatConnector;

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
        return httpClient.sendMessage(token, chat, textMessage).doOnSuccess(outgoingMessages$::onNext).ignoreElement();
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
                .mergeWith(outgoingMessages$);
    }

    @Override
    final public void connectMessagesToChat(Observable<String> messages, Observable<Chat> chat, Consumer<String> chatNotSetHandler) {
        if (messageToChatConnector != null) throw new IllegalStateException("Already connected");
        messageToChatConnector = ensureChatIsAssignedAndConnect(messages, chat, this::sendMessage, chatNotSetHandler);
    }

    @Override
    final public void close() throws Exception {
        if (messageToChatConnector != null) messageToChatConnector.dispose();
        outgoingMessages$.onComplete();
        httpClient.close();
    }

    private static Disposable ensureChatIsAssignedAndConnect(Observable<String> messages, Observable<Chat> chat,
                                                             BiFunction<Chat, String, Completable> outgoingMessageHandler,
                                                             Consumer<String> chatNotSetHandler) {
        Completable notifyChatNotSet = messages.takeUntil(chat).doOnNext(chatNotSetHandler).ignoreElements();
        Observable<String> outgoingMessage = messages.skipUntil(chat).mergeWith(notifyChatNotSet);
        return Observable.combineLatest(chat, outgoingMessage, outgoingMessageHandler)
                .flatMapCompletable(c -> c).subscribe();
    }
}
