package telegrambot.pollingbot;

import io.reactivex.Observable;
import io.reactivex.Single;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.httpclient.BotApiHttpClient;

import java.util.concurrent.TimeUnit;

public class LongPollingBot extends ShortPollingBot {
    private final static int POLLING_TIMEOUT = 60;
    private final static int POLLING_REPEAT_DELAY = 5;
    private final static int POLLING_RETRY_DELAY = 10;

    private final UpdateOffsetHolder updateOffset = new UpdateOffsetHolder();

    public LongPollingBot(String token, BotApiHttpClient httpClient) {
        super(token, httpClient);
    }

    private String longPollingQueryAck() {
        String offs = updateOffset.isSet() ? String.format("&offset=%d", updateOffset.getNext()) : "";
        return String.format("timeout=%d%s", POLLING_TIMEOUT, offs);
    }

    private Observable<Message> handleUpdates(Update[] updates) {
        updateOffset.refresh(updates);
        return Observable.fromArray(updates).map(Update::getMessage);
    }

    @Override
    public Observable<Message> getMessages() {
        return Single.defer(() -> getUpdates(longPollingQueryAck()))
                .flatMapObservable(this::handleUpdates)
                .repeatWhen(handler -> handler.delay(POLLING_REPEAT_DELAY, TimeUnit.SECONDS))
                .retryWhen(handler -> handler.delay(POLLING_RETRY_DELAY, TimeUnit.SECONDS));
    }
}
