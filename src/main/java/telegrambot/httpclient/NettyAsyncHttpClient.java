package telegrambot.httpclient;

import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.Single;
import io.reactivex.subjects.ReplaySubject;
import org.asynchttpclient.*;

import java.io.ByteArrayOutputStream;

final public class NettyAsyncHttpClient extends AbstractHttpClientApiAdapter {

    private final AsyncHttpClient httpClient;

    NettyAsyncHttpClient() {
        httpClient = Dsl.asyncHttpClient();
    }

    private static String apiUri(String token, String method) {
        return String.format("https://api.telegram.org/bot%s/%s", token, method);
    }

    private Single<byte[]> toSingle(Request request) {
        ReplaySubject<byte[]> bodyParts = ReplaySubject.create();
        httpClient.executeRequest(request, new RxAsyncHandler(bodyParts));
        return bodyParts.collectInto(new ByteArrayOutputStream(), (bout, bytes) -> bout.write(bytes, 0, bytes.length))
                .map(ByteArrayOutputStream::toByteArray);
    }

    @Override
    Single<byte[]> getRequest(String token, String method, String query) {
        if (query != null && !query.isEmpty()) method += "?" + query;
        return toSingle(Dsl
                .get(apiUri(token, method))
                .build());
    }

    @Override
    Single<byte[]> postRequest(String token, String method, String json) {
        return toSingle(Dsl
                .post(apiUri(token, method))
                .setBody(json)
                .setHeader("Content-Type", "application/json")
                .build());
    }

    @Override
    public void close() {
        // this implementation does nothing
    }

    static private class RxAsyncHandler implements AsyncHandler<Void> {

        private final ReplaySubject<byte[]> bodyParts;

        RxAsyncHandler(ReplaySubject<byte[]> bodyParts) {
            this.bodyParts = bodyParts;
        }

        @Override
        public State onStatusReceived(HttpResponseStatus responseStatus) {
            return State.CONTINUE;
        }

        @Override
        public State onHeadersReceived(HttpHeaders headers) {
            return State.CONTINUE;
        }

        @Override
        public State onBodyPartReceived(HttpResponseBodyPart bodyPart) {
            bodyParts.onNext(bodyPart.getBodyPartBytes().clone());
            return State.CONTINUE;
        }

        @Override
        public void onThrowable(Throwable t) {
            bodyParts.onError(t);
        }

        @Override
        public Void onCompleted() {
            bodyParts.onComplete();
            return null;
        }
    }

}
