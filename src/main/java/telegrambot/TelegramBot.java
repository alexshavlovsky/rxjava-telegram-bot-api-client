package telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;
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

import static telegrambot.io.ApiHttpClient.parseHttpResponse;


public class TelegramBot implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final static int POLLING_TIMEOUT = 60;
    private final static int POLLING_REPEAT_DELAY = 5;
    private final static int POLLING_RETRY_DELAY = 10;
    private final BotService botService;
    private final TokenStorageService tokenStorageService;
    private final User thisBot;
    private final ReplaySubject<Chat> currentChat = ReplaySubject.create();
    private final Observable<Chat> currentChatObservable = currentChat.asObservable().distinctUntilChanged();

    private final ApiHttpClient http;
    private final UpdateOffsetHolder updateOffset = new UpdateOffsetHolder();

    public TelegramBot(String token) {
        tokenStorageService = new TokenStorageService();
        if (token == null) {
            logger.info("Try to load a token from the file system...");
            token = tokenStorageService.getMostRecentToken();
            if (token == null) throw BotException.NO_SAVED_TOKEN;
        }
        http = new ApiHttpClient();
        logger.info("Validate the token against Telegram API...");
        thisBot = http.validateTokenAgainstApiBlocking(token);
        if (thisBot == null) throw BotException.TOKEN_VALIDATION_ERROR;
        tokenStorageService.saveToken(token);
        botService = new BotService(token);
        botService.saveUser(thisBot);
        logger.info("Current bot name {}", getBotName());
        // set current chat
        botService.getMessages().stream().max(Comparator.comparing(Message::getDate))
                .ifPresent(message -> currentChat.onNext(message.getChat()));
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

    private Observable<String> messageUpdatesObservable() {
        return Observable
                .defer(() -> parseHttpResponse(
                        http.apiGetRequest(botService.getToken(), createGetUpdatesMethodWithQuery()), Update[].class))
                .flatMap(this::handleUpdates)
                .doOnNext(botService::saveMessage)
                .doOnNext(message -> currentChat.onNext(message.getChat()))
                .map(message -> MessageFormatter.format(message, botService))
                .doOnError(e -> logger.error("API updates polling: {}", e.getMessage()))
                .repeatWhen(completed -> completed.delay(POLLING_REPEAT_DELAY, TimeUnit.SECONDS))
                .retryWhen(completed -> completed.delay(POLLING_RETRY_DELAY, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.io());
    }

    private Observable<String> messageHistoryObservable() {
        return Observable
                .from(botService.getMessages().stream().sorted(Comparator.comparing(Message::getDate)).collect(Collectors.toList()))
                .map(m -> MessageFormatter.format(m, botService));
    }

    public Observable<String> messageObservable() {
        return messageHistoryObservable().concatWith(messageUpdatesObservable());
    }

    public Observable<String> sendMessage(Long chatId, String text) {
        var json = String.format("{\"chat_id\":%d,\"text\":\"%s\"}", chatId, text);
        var response = http.apiPostRequest(botService.getToken(), "sendMessage", json);
        return parseHttpResponse(response, Message.class)
                .doOnNext(botService::saveMessage)
                .map(m -> MessageFormatter.format(m, botService))
                .doOnError(e -> logger.error("Send message: {}", e.getMessage()))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public void close() throws IOException {
        http.close();
    }

    public Observable<Chat> currentChatObservable() {
        return currentChatObservable;
    }

    public String getCurrentToken() {
        return botService.getToken();
    }

    public String getBotName() {
        return MessageFormatter.formatName(thisBot);
    }
}
