package telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.apimodel.User;
import telegrambot.io.ApiHttpClient;
import telegrambot.io.BotService;
import telegrambot.io.TokenStorageService;
import telegrambot.io.UpdateOffsetHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static telegrambot.io.ApiHttpClient.parseApiHttpResponse;

public class TelegramBot implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final static int POLLING_TIMEOUT = 60;
    private final static int POLLING_REPEAT_DELAY = 5;
    private final static int POLLING_RETRY_DELAY = 10;
    private final BotService botService;
    private final ReplaySubject<Chat> currentChatSubject = ReplaySubject.create();
    private final PublishSubject<String> userInputSubject = PublishSubject.create();
    private final Observable<Chat> currentChatObservable = currentChatSubject.asObservable().distinctUntilChanged();
    private final Observable<String> userInputObservable = userInputSubject.asObservable();
    private final String token;
    private final User botUser;

    private final ApiHttpClient http;
    private final UpdateOffsetHolder updateOffset = new UpdateOffsetHolder();

    public TelegramBot(String token) throws BotException, IOException {
        TokenStorageService tokenStorageService = new TokenStorageService();
        if (token == null) {
            logger.info("Try to load a token from the file system...");
            token = tokenStorageService.getMostRecentToken();
            if (token == null) throw BotException.NO_SAVED_TOKEN;
        }
        logger.info("Validate the token against Telegram API...");
        http = new ApiHttpClient();
        botUser = validateTokenAgainstApiBlocking(token);
        if (botUser == null) {
            http.close();
            throw BotException.TOKEN_VALIDATION_ERROR;
        }
        this.token = token;
        tokenStorageService.saveToken(token);
        botService = new BotService(token);
        botService.saveUser(botUser);
        // set current chat
        botService.getMessages().stream().max(Comparator.comparing(Message::getDate))
                .ifPresent(message -> currentChatSubject.onNext(message.getChat()));
    }

    private String createGetUpdatesMethodWithQuery() {
        var uri = String.format("getUpdates?timeout=%d", POLLING_TIMEOUT);
        if (updateOffset.isSet()) uri += String.format("&offset=%d", updateOffset.getNext());
        return uri;
    }

    private Observable<Message> handleUpdates(Update[] updates) {
        updateOffset.refresh(updates);
        return Observable.from(Arrays.stream(updates).map(Update::getMessage).collect(Collectors.toList()));
    }

    private void handleIncomingMessage(Message message) {
        botService.saveMessage(message);
        currentChatSubject.onNext(message.getChat());
    }

    private String formatMessage(Message message) {
        return MessageFormatter.formatMessage(message, botService, botUser);
    }

    private Observable<String> incomingMessageObservable() {
        return Observable
                .defer(() -> parseApiHttpResponse(
                        http.apiGetRequest(token, createGetUpdatesMethodWithQuery()), Update[].class))
                .flatMap(this::handleUpdates)
                .doOnNext(this::handleIncomingMessage)
                .map(this::formatMessage)
                .doOnError(e -> logger.error("API updates polling: {}", e.getMessage()))
                .repeatWhen(completed -> completed.delay(POLLING_REPEAT_DELAY, TimeUnit.SECONDS))
                .retryWhen(completed -> completed.delay(POLLING_RETRY_DELAY, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.io());
    }

    private Observable<String> messageHistoryObservable() {
        var messages = botService.getMessages().stream().sorted(Comparator.comparing(Message::getDate)).collect(Collectors.toList());
        return Observable.from(messages).map(this::formatMessage);
    }

    private Observable<String> outgoingMessageAcknowledgmentObservable() {
        return Observable.combineLatest(currentChatObservable, userInputObservable,
                (a, b) -> sendMessage(a.getId(), b)).flatMap(o -> o);
    }

    public Observable<String> messageObservable() {
        return messageHistoryObservable()
                .concatWith(incomingMessageObservable()
                        .mergeWith(outgoingMessageAcknowledgmentObservable()));
    }

    public void sendMessage(String text) {
        userInputSubject.onNext(text);
    }

    private Observable<String> sendMessage(Long chatId, String text) {
        var json = String.format("{\"chat_id\":%d,\"text\":\"%s\"}", chatId, text);
        var response = http.apiPostRequest(token, "sendMessage", json);
        return parseApiHttpResponse(response, Message.class)
                .doOnNext(botService::saveMessage)
                .map(this::formatMessage)
                .doOnError(e -> logger.error("Send message: {}", e.getMessage()))
                .subscribeOn(Schedulers.io());
    }

    private User validateTokenAgainstApiBlocking(String token) {
        return parseApiHttpResponse(http.apiGetRequest(token, "getMe"), User.class)
                .doOnError(e -> logger.error("Validate token against API: {}", e.getMessage()))
                .onErrorReturn((e) -> null)
                .toBlocking().single();
    }

    @Override
    public void close() throws IOException {
        http.close();
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
}
