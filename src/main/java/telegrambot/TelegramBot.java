package telegrambot;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.User;
import telegrambot.httpclient.BotApiHttpClientFactory;
import telegrambot.httpclient.BotApiHttpClientType;
import telegrambot.io.BotService;
import telegrambot.io.TokenStorageService;
import telegrambot.pollingclient.LongPollingClient;
import telegrambot.pollingclient.PollingClient;

import java.util.Comparator;

public class TelegramBot implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final BotService botService;
    private final User botUser;

    private final PollingClient pollingClient;

    private final ReplaySubject<Chat> latestChat$ = ReplaySubject.create();
    private final PublishSubject<String> outgoingTextMessages$ = PublishSubject.create();
    private final Observable<Chat> latestChatObservable = latestChat$.distinctUntilChanged();

    public TelegramBot(String token, BotApiHttpClientType clientType) throws BotException {
        TokenStorageService tokenStorageService = new TokenStorageService();
        if (token == null) {
            logger.info("Try to load a token from the file system...");
            token = tokenStorageService.getMostRecentToken();
            if (token == null) throw new BotException(
                    "Can't find any saved token.\nPlease provide an API token via command line argument.\nYou can get one from BotFather.");
        }
        logger.info("Validate the token against Telegram API...");
        pollingClient = new LongPollingClient(token, BotApiHttpClientFactory.newInstance(clientType));
        try {
            botUser = pollingClient.getMe().blockingGet();
        } catch (Exception e) {
            try {
                pollingClient.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw new BotException("Unable to validate token against Telegram API: " + e.getMessage(), e);
        }
        tokenStorageService.saveToken(token);
        botService = new BotService(token);
        botService.saveUser(botUser);
        logger.info("Current bot name: {}", MessageFormatter.formatName(botUser));
        // publish the latest chat from bot service if any
        botService.getMessages().stream().max(Comparator.comparing(Message::getDate))
                .ifPresent(message -> latestChat$.onNext(message.getChat()));
        // stream outgoing messages to the latest chat
        pollingClient.connectMessagesToChat(outgoingTextMessages$, latestChatObservable,
                m -> logger.info("A current chat is not assigned. Please send a message to this bot first!"));
    }

    private Observable<Message> newMessagesObservable() {
        return pollingClient
                .pollMessages()
                .doOnNext(botService::saveMessage)
                .doOnNext(message -> latestChat$.onNext(message.getChat()));
    }

    private Observable<Message> messageHistoryObservable() {
        return Observable.fromIterable(botService.getMessages()).sorted(Comparator.comparing(Message::getDate));
    }

    public Observable<String> messageObservable() {
        return messageHistoryObservable().concatWith(newMessagesObservable())
                .map(m -> MessageFormatter.formatMessage(m, botService, botUser));
    }

    public void sendMessage(String text) {
        outgoingTextMessages$.onNext(text);
    }

    public Observable<String> currentChatObservable() {
        return latestChatObservable.map(MessageFormatter::formatChat);
    }

    @Override
    public void close() throws Exception {
        latestChat$.onComplete();
        outgoingTextMessages$.onComplete();
        pollingClient.close();
    }
}
