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
import telegrambot.apimodel.User;
import telegrambot.pollingbot.LongPollingBot;
import telegrambot.pollingbot.PollingBot;
import telegrambot.httpclient.BotApiHttpClientFactory;
import telegrambot.httpclient.BotApiHttpClientType;
import telegrambot.io.BotService;
import telegrambot.io.TokenStorageService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TelegramBot implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final BotService botService;
    private final User botUser;

    private final PollingBot pollingBot;

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

    public TelegramBot(String token, BotApiHttpClientType clientType) throws BotException {
        TokenStorageService tokenStorageService = new TokenStorageService();
        if (token == null) {
            logger.info("Try to load a token from the file system...");
            token = tokenStorageService.getMostRecentToken();
            if (token == null) throw new BotException(
                    "Can't find any saved token.\nPlease provide an API token via command line argument.\nYou can get one from BotFather.");
        }
        logger.info("Validate the token against Telegram API...");
        pollingBot = new LongPollingBot(token, BotApiHttpClientFactory.newInstance(clientType));
        try {
            botUser = pollingBot.getMe().blockingGet();
        } catch (Exception e) {
            try {
                pollingBot.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw new BotException("Unable to validate token against Telegram API: " + e.getMessage(), e);
        }
        tokenStorageService.saveToken(token);
        botService = new BotService(token);
        botService.saveUser(botUser);
        logger.info("Current bot name: {}", MessageFormatter.formatName(botUser));
        // publish the latest chat from bot service
        botService.getMessages().stream().max(Comparator.comparing(Message::getDate))
                .ifPresent(message -> currentChatSubject.onNext(message.getChat()));
    }

    private Single<Message> sendMessageAckObservable(Chat chat, String textMessage) {
        return pollingBot.sendMessage(chat, textMessage)
                .doOnSuccess(botService::saveMessage)
                .onErrorResumeNext(e -> {
                    logger.error("Send message error: {}", e.getMessage());
                    return Single.never();
                });
    }

    private void handleMessage(Message message) {
        // update the current chat
        currentChatSubject.onNext(message.getChat());
        // persist incoming messages
        botService.saveMessage(message);
    }

    private Observable<Message> incomingMessageObservable() {
        return pollingBot.getMessages().doOnNext(this::handleMessage);
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

    @Override
    public void close() throws Exception {
        pollingBot.close();
    }
}
