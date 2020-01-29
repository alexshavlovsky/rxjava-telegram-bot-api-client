package telegrambot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.apache.http.ObservableHttp;
import rx.apache.http.ObservableHttpResponse;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;
import telegrambot.apimodel.*;
import telegrambot.io.BotService;
import telegrambot.io.TokenStorageService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
    private final TokenStorageService tokenStorageService;
    private final User thisBot;
    private final ReplaySubject<Chat> currentChat = ReplaySubject.create();
    private final Observable<Chat> currentChatObservable = currentChat.asObservable().distinctUntilChanged();
    private long updateOffset = -1;

    private final ObjectMapper objectMapper;
    private final CloseableHttpAsyncClient httpClient;

    public TelegramBot(String token) {
        tokenStorageService = new TokenStorageService();
        if (token == null) {
            logger.info("Try to load a token from the file system...");
            token = tokenStorageService.getMostRecentToken();
            if (token == null) throw BotException.NO_SAVED_TOKEN;
        }
        // initialize ObjectMapper and HTTP client
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
        logger.info("Validate the token against Telegram API...");
        thisBot = resolveBotByTokenBlocking(token);
        if (thisBot == null) throw BotException.TOKEN_VALIDATION_ERROR;
        // save valid token to storage
        tokenStorageService.saveToken(token);
        // create a bot service
        botService = new BotService(token);
        botService.saveUser(thisBot);
        logger.info("Current bot name {}", getBotName());
        // set current chat
        botService.getMessages().stream().max(Comparator.comparing(Message::getDate))
                .ifPresent(message -> currentChat.onNext(message.getChat()));
    }

    private static String apiUri(String token, String method) {
        return String.format("https://api.telegram.org/bot%s/%s", token, method);
    }

    private Observable<ObservableHttpResponse> apiGetRequest(String token, String method) {
        var uri = apiUri(token, method);
        var producer = HttpAsyncMethods.createGet(uri);
        return ObservableHttp.createRequest(producer, httpClient).toObservable();
    }

    private Observable<ObservableHttpResponse> apiPostRequest(String token, String method, String json) {
        try {
            var uri = apiUri(token, method);
            var producer = HttpAsyncMethods.createPost(uri, json, ContentType.APPLICATION_JSON);
            return ObservableHttp.createRequest(producer, httpClient).toObservable();
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
    }

    private <T> Observable<ApiResponse<T>> parseJson(byte[] response, Class<T> valueType) {
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, valueType);
        try {
            return Observable.just(objectMapper.readValue(response, type));
        } catch (Exception e) {
            return Observable.error(e);
        }
    }

    private <T extends ApiResponse> T catchAndPropagateApiError(T response) {
        if (!response.getOk()) throw new RuntimeException(response.toString());
        return response;
    }

    private <T> Observable<T> parseHttpResponse(Observable<ObservableHttpResponse> response, Class<T> valueType) {
        return response
                .flatMap(ObservableHttpResponse::getContent)
                .flatMap(content -> parseJson(content, valueType))
                .map(this::catchAndPropagateApiError)
                .map(ApiResponse::getResult);
    }

    private List<Message> handleUpdates(Update[] updates) {
        refreshUpdateOffset(updates);
        return Arrays.stream(updates).map(Update::getMessage).collect(Collectors.toList());
    }

    private void refreshUpdateOffset(Update[] updates) {
        updateOffset = updates.length > 0 ? updates[updates.length - 1].getUpdate_id() : -1;
    }

    private static String getUpdatesMethod(long updateOffset) {
        var uri = "getUpdates";
        if (updateOffset == -1) return uri;
        return uri + String.format("?offset=%d&timeout=%d", updateOffset + 1, POLLING_TIMEOUT);
    }

    public Observable<String> messageHistoryObservable() {
        return Observable
                .from(botService.getMessages().stream().sorted(Comparator.comparing(Message::getDate)).collect(Collectors.toList()))
                .map(m -> MessageFormatter.format(m, botService));
    }

    public Observable<String> messageUpdatesObservable() {
        return Observable
                .defer(() -> parseHttpResponse(apiGetRequest(botService.getToken(), getUpdatesMethod(updateOffset)), Update[].class))
                .map(this::handleUpdates)
                .flatMap(Observable::from)
                .doOnNext(botService::saveMessage)
                .doOnNext(message -> currentChat.onNext(message.getChat()))
                .map(message -> MessageFormatter.format(message, botService))
                .doOnError(e -> logger.error("API updates polling: {}", e.getMessage()))
                .repeatWhen(completed -> completed.delay(POLLING_REPEAT_DELAY, TimeUnit.SECONDS))
                .retryWhen(completed -> completed.delay(POLLING_RETRY_DELAY, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.io());
    }

    public Observable<String> sendMessage(Long chatId, String text) {
        var json = String.format("{\"chat_id\":%d,\"text\":\"%s\"}", chatId, text);
        var response = apiPostRequest(botService.getToken(), "sendMessage", json);
        return parseHttpResponse(response, Message.class)
                .doOnNext(botService::saveMessage)
                .map(m -> MessageFormatter.format(m, botService))
                .doOnError(e -> logger.error("Send message: {}", e.getMessage()))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    public Observable<Chat> currentChatObservable() {
        return currentChatObservable;
    }

    private User resolveBotByTokenBlocking(String token) {
        return parseHttpResponse(apiGetRequest(token, "getMe"), User.class)
                .doOnError(e -> logger.error("Resolve token against API: {}", e.getMessage()))
                .onErrorReturn((e) -> null)
                .toBlocking().single();
    }

    public String getCurrentToken() {
        return botService.getToken();
    }

    public String getBotName() {
        return MessageFormatter.formatName(thisBot);
    }
}
