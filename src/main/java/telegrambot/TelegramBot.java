package telegrambot;


import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;
import telegrambot.httpclient.HttpClientFactory;
import telegrambot.httpclient.HttpClient;
import telegrambot.httpclient.HttpClientType;
import telegrambot.io.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TelegramBot implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final static int POLLING_TIMEOUT = 60;
    private final static int POLLING_REPEAT_DELAY = 5;
    private final static int POLLING_RETRY_DELAY = 10;

    private final BotService botService;
    private final String token;
    private final User botUser;

    private final HttpClient http;
    private final UpdateOffsetHolder updateOffset = new UpdateOffsetHolder();

    private final ReplaySubject<Chat> currentChatSubject = ReplaySubject.create();
    private final PublishSubject<String> userTextInputSubject = PublishSubject.create();
    private final Observable<Chat> currentChatObservable = currentChatSubject.distinctUntilChanged();

    private final Completable notifyChatNotSet = userTextInputSubject
            .takeUntil(currentChatObservable)
            .doOnNext(m -> logger.info("A current chat is not assigned. Please send a message to this bot first!"))
            .ignoreElements();

    private final Observable<String> outgoingUserText = userTextInputSubject
            .skipUntil(currentChatObservable)
            .mergeWith(notifyChatNotSet);

    private final Observable<Message> outgoingMessageAckObservable = Observable
            .combineLatest(
                    currentChatObservable,
                    outgoingUserText,
                    this::sendMessageAckObservable)
            .flatMapSingle(o -> o);

    public TelegramBot(String token, HttpClientType clientType) throws Exception {
        TokenStorageService tokenStorageService = new TokenStorageService();
        if (token == null) {
            logger.info("Try to load a token from the file system...");
            token = tokenStorageService.getMostRecentToken();
            if (token == null) throw BotException.NO_SAVED_TOKEN;
        }
        logger.info("Validate the token against Telegram API...");
        http = HttpClientFactory.newInstance(clientType);
        try {
            botUser = validateTokenAgainstApi(token).blockingGet();
        } catch (Exception e) {
            http.close();
            throw new BotException("Unable to validate token against Telegram API: " + e.getMessage(), e);
        }
        if (botUser == null) {
            http.close();
            throw new BotException("Unable to validate token against Telegram API: Bot user is null");
        }
        this.token = token;
        tokenStorageService.saveToken(token);
        botService = new BotService(token);
        botService.saveUser(botUser);
        logger.info("Current bot name: {}", MessageFormatter.formatName(botUser));
        // publish the latest chat from bot service
        botService.getMessages().stream().max(Comparator.comparing(Message::getDate))
                .ifPresent(message -> currentChatSubject.onNext(message.getChat()));
    }

    private Single<User> validateTokenAgainstApi(String token) {
        return http.apiGetRequest(token, "getMe", "", User.class)
                .doOnError(e -> logger.error("Validate token against API: {}", e.getMessage()));
    }

    private Single<Message> sendMessageAckObservable(Chat chat, String text) {
        String json = String.format("{\"chat_id\":%d,\"text\":\"%s\"}", chat.getId(), text);
        return http.apiPostRequest(token, "sendMessage", json, Message.class)
                .doOnSuccess(botService::saveMessage)
                .doOnError(e -> logger.error("Send message error: {}", e.getMessage()))
                .onErrorResumeNext(e -> Single.never());
    }

    private String createGetUpdatesQuery() {
        String offs = updateOffset.isSet() ? String.format("&offset=%d", updateOffset.getNext()) : "";
        return String.format("timeout=%d%s", POLLING_TIMEOUT, offs);
    }

    private Observable<Message> handleUpdates(Update[] updates) {
        // refresh the update offset
        updateOffset.refresh(updates);
        List<Message> messages = Arrays.stream(updates).map(Update::getMessage).collect(Collectors.toList());
        // update the current chat
        messages.forEach(m -> currentChatSubject.onNext(m.getChat()));
        // persist incoming messages
        botService.saveAllMessages(messages);
        return Observable.fromIterable(messages);
    }

    // TODO: handle native connection exceptions
    private Observable<Message> incomingMessageObservable() {
        return Single.defer(() -> http.apiGetRequest(token, "getUpdates", createGetUpdatesQuery(), Update[].class))
                .flatMapObservable(this::handleUpdates)
                .doOnError(e -> logger.error("API updates polling: {}", e.getMessage()))
                .repeatWhen(completed -> completed.delay(POLLING_REPEAT_DELAY, TimeUnit.SECONDS))
                .retryWhen(completed -> completed.delay(POLLING_RETRY_DELAY, TimeUnit.SECONDS));
    }

    private Observable<Message> messageHistoryObservable() {
        List<Message> messages = botService
                .getMessages().stream().sorted(Comparator.comparing(Message::getDate)).collect(Collectors.toList());
        return Observable.fromIterable(messages);
    }

    public Observable<String> messageObservable() {
        return messageHistoryObservable()
                .concatWith(incomingMessageObservable().mergeWith(outgoingMessageAckObservable))
                .map(m -> MessageFormatter.formatMessage(m, botService, botUser));
    }

    public void sendMessage(String text) {
        userTextInputSubject.onNext(text);
    }

    public Observable<String> currentChatObservable() {
        return currentChatObservable.map(MessageFormatter::formatChat);
    }

    public String getToken() {
        return token;
    }

    public String getBotName() {
        return MessageFormatter.formatName(botUser);
    }

    @Override
    public void close() throws Exception {
        http.close();
    }
}
