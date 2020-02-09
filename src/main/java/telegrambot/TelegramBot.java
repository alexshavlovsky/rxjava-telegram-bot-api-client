package telegrambot;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.User;
import telegrambot.httpclient.BotApiHttpClientFactory;
import telegrambot.httpclient.BotApiHttpClientType;
import telegrambot.io.BotRepository;
import telegrambot.io.TokensRepository;
import telegrambot.pollingclient.LongPollingClient;
import telegrambot.pollingclient.PollingClient;

public class TelegramBot implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final BotRepository botRepository;
    private final User botUser;

    private final PollingClient pollingClient;

    private final PublishSubject<Chat> latestChat$ = PublishSubject.create();
    private final PublishSubject<String> outgoingTextMessages$ = PublishSubject.create();

    public TelegramBot(String token, BotApiHttpClientType clientType) throws BotException {
        TokensRepository tokensRepository = new TokensRepository();
        // if token is not provided, try to load most recent used token
        if (token == null) {
            logger.info("Try to load a token from the file system...");
            token = tokensRepository.getMostRecentToken();
            if (token == null) throw new BotException(
                    "Can't find any saved token.\nPlease provide an API token via command line argument.\nYou can get one from BotFather.");
        }
        // create an http client instance
        pollingClient = new LongPollingClient(token, BotApiHttpClientFactory.newInstance(clientType));
        // if the repo contains this token, then use it or else validate it against API
        if (tokensRepository.containsToken(token)) botUser = tokensRepository.getUserForToken(token);
        else {
            logger.info("Validate a new token against Telegram API...");
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
            tokensRepository.saveToken(token, botUser);
        }
        botRepository = new BotRepository(token);
        botRepository.saveUser(botUser);
        // log the current bot name
        logger.info("Current bot name: {}", MessageFormatter.formatName(botUser));
        Observable<Chat> latestChatObservable = latestChat$.distinctUntilChanged();
        // stream the latest chat on change events to logger
        latestChatObservable.map(MessageFormatter::formatChat)
                .subscribe(chat -> logger.info("Current chat is set to: {}", chat));
        // stream outgoing messages to the latest chat
        pollingClient.connectMessagesToChat(outgoingTextMessages$, latestChatObservable,
                m -> logger.info("A current chat is not assigned. Please send a message to this bot first!"));
        // publish the latest chat from bot service if any
        botRepository.getLatestChatOptional().ifPresent(latestChat$::onNext);
    }

    private Observable<Message> newMessagesObservable() {
        return pollingClient.pollMessages()
                .doOnNext(botRepository::saveMessage)
                .doOnNext(message -> latestChat$.onNext(message.getChat()));
    }

    public Observable<String> messageObservable() {
        return botRepository.messageHistoryOrderedObservable().concatWith(newMessagesObservable())
                .map(m -> MessageFormatter.formatMessage(m, botRepository, botUser));
    }

    public void sendMessage(String text) {
        outgoingTextMessages$.onNext(text);
    }

    @Override
    public void close() throws Exception {
        latestChat$.onComplete();
        outgoingTextMessages$.onComplete();
        pollingClient.close();
    }
}
