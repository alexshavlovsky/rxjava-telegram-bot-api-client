package telegrambot;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import telegrambot.apimodel.Chat;
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

    private final ReplaySubject<Chat> latestChat$ = ReplaySubject.create();

    public TelegramBot(String token, BotApiHttpClientType clientType) throws BotException {
        TokensRepository tokensRepository = new TokensRepository();

        if (token == null) {
            logger.info("Try to load a token from the file system...");
            token = tokensRepository.getMostRecentToken();
            if (token == null) throw new BotException(
                    "Can't find any saved token.\nPlease provide an API token via command line argument.\nYou can get one from BotFather.");
        }

        pollingClient = new LongPollingClient(token, BotApiHttpClientFactory.newInstance(clientType), logger);

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

        logger.info("Current bot name: {}", MessageFormatter.formatName(botUser));

        botRepository.getLatestChatOptional().ifPresent(latestChat$::onNext);
    }

    public Observable<String> messageObservable(Observable<String> outgoingTextMessages) {

        Observable<Chat> latestChatObservable = latestChat$.distinctUntilChanged().takeUntil(outgoingTextMessages.lastElement().toObservable());

        return botRepository.messageHistoryOrderedObservable()

                .concatWith(
                        pollingClient
                                .connect(outgoingTextMessages, latestChatObservable,
                                        lostMessage -> logger.info("A current chat is not assigned. Please send a message to this bot first!"))
                                .doOnNext(botRepository::saveMessage)
                                .doOnNext(message -> latestChat$.onNext(message.getChat()))
                )

                .map(message -> MessageFormatter.formatMessage(message, botRepository, botUser))

                .mergeWith(
                        latestChatObservable.map(MessageFormatter::formatChat)
                                .doOnNext(chat -> logger.info("Current chat is set to: {}", chat))
                                .ignoreElements()
                );
    }

    @Override
    public void close() throws Exception {
        latestChat$.onComplete();
        pollingClient.close();
    }
}
