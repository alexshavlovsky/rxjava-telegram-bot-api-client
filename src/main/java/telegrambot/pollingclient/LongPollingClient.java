package telegrambot.pollingclient;

import io.reactivex.Observable;
import io.reactivex.Single;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.Update;
import telegrambot.httpclient.BotApiHttpClient;

import java.util.concurrent.TimeUnit;

public class LongPollingClient extends ShortPollingClient {
    private final static int POLLING_TIMEOUT = 60;
    private final static int POLLING_REPEAT_DELAY = 1;
    private final static int POLLING_RETRY_DELAY = 10;

    private final UpdateOffsetHolder updateOffset = new UpdateOffsetHolder();

    public LongPollingClient(String token, BotApiHttpClient httpClient) {
        super(token, httpClient);
    }

    private String longPollingQueryWithAckOffset() {
        String offs = updateOffset.isSet() ? String.format("&offset=%d", updateOffset.getNext()) : "";
        return String.format("timeout=%d%s", POLLING_TIMEOUT, offs);
    }

    private Observable<Message> handleUpdates(Update[] updates) {
        updateOffset.refresh(updates);
        return Observable.fromArray(updates).map(Update::getMessage);
    }

    @Override
    public Observable<Message> pollMessages() {
        return Single.defer(() -> getUpdates(longPollingQueryWithAckOffset()))
                .flatMapObservable(this::handleUpdates)
                .repeatWhen(handler -> handler.takeUntil(outgoingMessages).delay(POLLING_REPEAT_DELAY, TimeUnit.SECONDS))
                .retryWhen(handler -> handler.takeUntil(outgoingMessages).delay(POLLING_RETRY_DELAY, TimeUnit.SECONDS))
                .mergeWith(outgoingMessages);
    }
}
