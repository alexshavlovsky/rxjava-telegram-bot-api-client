package telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.apache.http.ObservableHttpResponse;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static telegrambot.io.ApiHttpClient.parseApiHttpResponse;

public class TelegramBot implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final static int POLLING_TIMEOUT = 60;
    private final static int POLLING_REPEAT_DELAY = 5;
    private final static int POLLING_RETRY_DELAY = 10;

    private final BotService botService;
    private final String token;
    private final User botUser;

    private final ApiHttpClient http;
    private final UpdateOffsetHolder updateOffset = new UpdateOffsetHolder();

    private final ReplaySubject<Chat> currentChatSubject = ReplaySubject.create();
    private final PublishSubject<String> userTextInputSubject = PublishSubject.create();
    private final Observable<Chat> currentChatObservable = currentChatSubject.distinctUntilChanged();

    private final Observable<String> notifyChatNotSet = userTextInputSubject
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
            .flatMap(o -> o);

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
        logger.info("Current bot name: {}", MessageFormatter.formatName(botUser));
        // publish the latest chat from bot service
        botService.getMessages().stream().max(Comparator.comparing(Message::getDate))
                .ifPresent(message -> currentChatSubject.onNext(message.getChat()));
    }

    private User validateTokenAgainstApiBlocking(String token) {
        return parseApiHttpResponse(http.apiGetRequest(token, "getMe"), User.class)
                .doOnError(e -> logger.error("Validate token against API: {}", e.getMessage()))
                .onErrorReturn((e) -> null)
                .toBlocking().single();
    }

    private Observable<Message> sendMessageAckObservable(Chat chat, String text) {
        String json = String.format("{\"chat_id\":%d,\"text\":\"%s\"}", chat.getId(), text);
        Observable<ObservableHttpResponse> response = http.apiPostRequest(token, "sendMessage", json);
        return parseApiHttpResponse(response, Message.class)
                .doOnNext(botService::saveMessage)
                .doOnError(e -> logger.error("Send message: {}", e.getMessage()))
                .onExceptionResumeNext(Observable.empty())
                .onErrorResumeNext(Observable.empty());
    }

    private String createGetUpdatesMethodWithQuery() {
        String uri = String.format("getUpdates?timeout=%d", POLLING_TIMEOUT);
        if (updateOffset.isSet()) uri += String.format("&offset=%d", updateOffset.getNext());
        return uri;
    }

    private Observable<Message> handleUpdates(Update[] updates) {
        // refresh the update offset
        updateOffset.refresh(updates);
        List<Message> messages = Arrays.stream(updates).map(Update::getMessage).collect(Collectors.toList());
        // update the current chat
        messages.forEach(m -> currentChatSubject.onNext(m.getChat()));
        // persist incoming messages
        botService.saveAllMessages(messages);
        return Observable.from(messages);
    }

    private Observable<Message> incomingMessageObservable() {
        return Observable
                .defer(() -> parseApiHttpResponse(
                        http.apiGetRequest(token, createGetUpdatesMethodWithQuery()), Update[].class))
                .flatMap(this::handleUpdates)
                .doOnError(e -> logger.error("API updates polling: {}", e.getMessage()))
                .repeatWhen(completed -> completed.delay(POLLING_REPEAT_DELAY, TimeUnit.SECONDS))
                .retryWhen(completed -> completed.delay(POLLING_RETRY_DELAY, TimeUnit.SECONDS));
    }

    private Observable<Message> messageHistoryObservable() {
        List<Message> messages = botService
                .getMessages().stream().sorted(Comparator.comparing(Message::getDate)).collect(Collectors.toList());
        return Observable.from(messages);
    }

    public Observable<String> messageObservable() {
        return messageHistoryObservable()
                .concatWith(incomingMessageObservable().mergeWith(outgoingMessageAckObservable))
                .map(m -> MessageFormatter.formatMessage(m, botService, botUser));
    }

    public void sendMessage(String text) {
        userTextInputSubject.onNext(text);
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
